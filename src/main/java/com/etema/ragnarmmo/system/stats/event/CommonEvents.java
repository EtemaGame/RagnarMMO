package com.etema.ragnarmmo.system.stats.event;

import java.util.concurrent.ThreadLocalRandom;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.roitems.runtime.EquipmentCombatModifierResolver;
import com.etema.ragnarmmo.roitems.runtime.RoRefineMath;
import com.etema.ragnarmmo.roitems.runtime.WeaponStatHelper;
import com.etema.ragnarmmo.system.stats.RagnarStats;
import com.etema.ragnarmmo.system.stats.capability.PlayerStats;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import com.etema.ragnarmmo.system.stats.compute.StatComputer;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.stats.net.DerivedStatsSyncPacket;
import com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket;
import com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncService;
import com.etema.ragnarmmo.system.stats.progression.ExpTable;
import com.etema.ragnarmmo.system.stats.progression.JobBonusService;
import com.etema.ragnarmmo.system.stats.party.PartyXpService;
import com.etema.ragnarmmo.system.mobstats.util.MobUtils;
import com.google.common.collect.Multimap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MobType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID)
public class CommonEvents {
    // Use ThreadLocalRandom for thread safety in event handlers

    // Hardcoded balance constants (previously in BalanceConfig)
    // removed constants BASE_STAT_POINTS/POINTS_PER_LEVEL in favor of config
    private static final double MOB_EXP_HP_MULTIPLIER = 1.2; // Increased from 0.8
    private static final double MOB_EXP_ARMOR_BONUS = 2.5;  // Increased from 1.5
    private static final int MOB_EXP_MINIMUM = 5;
    private static final int MOB_EXP_MAXIMUM = 10000;
    private static final boolean MOB_EXP_SCALE_WITH_PLAYER_LEVEL = true;

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            RagnarCoreAPI.get(sp).ifPresent(s -> {
                double armaBase = getWeaponDamage(sp);
                var derived = StatComputer.compute(sp, s, armaBase, getWeaponAPS(sp), getWeaponMagicDamage(sp), getArmorEff(sp), 1.0);
                if (s instanceof PlayerStats internal) {
                    internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
                }
                JobBonusService.recomputeStats(sp, s);
                s.setManaMaxClient(derived.maxMana);
                if (s instanceof PlayerStats internal) {
                    internal.setSPMaxClient(derived.maxSP); // Fix: use SP-specific formula, not Mana
                }

                // Immediate HP sync on join (e.g. 20/20 -> 40/40)
                var hpAttr = sp.getAttribute(Attributes.MAX_HEALTH);
                if (hpAttr != null) {
                    float oldMax = (float) hpAttr.getBaseValue();
                    if (Math.abs(oldMax - derived.maxHealth) > 1e-4) {
                        boolean wasFull = sp.getHealth() >= oldMax - 0.01f;
                        hpAttr.setBaseValue(derived.maxHealth);
                        if (wasFull)
                            sp.setHealth((float) derived.maxHealth);
                    }
                }

                PlayerStatsSyncService.sync(sp, s);
                RagnarDebugLog.playerData(
                        "JOIN player={} baseLv={} jobLv={} job={} hp={} mana={}/{} sp={}/{}",
                        sp.getGameProfile().getName(),
                        s.getLevel(),
                        s.getJobLevel(),
                        s.getJobId(),
                        RagnarDebugLog.formatDouble(sp.getHealth()),
                        RagnarDebugLog.formatDouble(s.getMana()),
                        RagnarDebugLog.formatDouble(s.getManaMax()),
                        RagnarDebugLog.formatDouble(s.getSP()),
                        RagnarDebugLog.formatDouble(s.getSPMax()));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide)
            return;
        var p = (ServerPlayer) e.player;
        RagnarCoreAPI.get(p).ifPresent(stats -> {
            if (stats instanceof PlayerStats internal) {
                internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
            }
            var d = StatComputer.compute(p, stats, getWeaponDamage(p), getWeaponAPS(p), getWeaponMagicDamage(p), getArmorEff(p), 1.0);

            // Sync MAX_HEALTH
            var maxHealthInstance = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (maxHealthInstance != null) {
                double base = maxHealthInstance.getBaseValue();
                if (Math.abs(base - d.maxHealth) > 1e-4) {
                    boolean wasFull = p.getHealth() >= (float) base - 0.01f;
                    maxHealthInstance.setBaseValue(d.maxHealth);
                    if (wasFull || p.getHealth() > d.maxHealth) {
                        p.setHealth((float) d.maxHealth);
                    }
                }
            }

            double baseManaMax = d.maxMana;
            var manaAttr = p.getAttribute(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.MAX_MANA.get());
            double totalManaMax = baseManaMax + (manaAttr != null ? manaAttr.getValue() : 0.0);

            // SP uses its own formula (VIT/STR-based), not Mana's INT-based formula
            double baseSPMax = d.maxSP;
            var spAttr = p.getAttribute(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.MAX_SP.get());
            double totalSpMax = baseSPMax + (spAttr != null ? spAttr.getValue() : 0.0);

            double previousManaMax = stats.getManaMax();
            double previousSPMax = (stats instanceof PlayerStats internal) ? internal.getSPMax() : previousManaMax;

            if (Math.abs(previousManaMax - totalManaMax) > 1e-4 || Math.abs(previousSPMax - totalSpMax) > 1e-4) {
                stats.setManaMaxClient(totalManaMax);
                if (stats instanceof PlayerStats internal) {
                    internal.setSPMaxClient(totalSpMax);
                }
            }

            // --- Weight System ---
            int str = stats.getSTR();
            int maxWeight = 500 + (str * 50); // Minecraft-friendly curve
            int currentWeight = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack stack = p.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    int w = 1;
                    if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                            || stack.getItem() instanceof net.minecraft.world.item.TieredItem)
                        w = 10;
                    currentWeight += (stack.getCount() * w);
                }
            }

            boolean over50 = currentWeight > (maxWeight * 0.5);
            boolean over90 = currentWeight > (maxWeight * 0.9);

            if (over90 && p.tickCount % 20 == 0) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            }

            // Natural Regen is disabled if over 50% weight (RO Mechanic)
            if (!over50) {
                stats.addMana(d.manaRegenPerSecond / 20.0);
                if (stats instanceof PlayerStats internal) {
                    internal.addSP(d.spRegenPerSecond / 20.0); // Fix: use SP-specific regen, not mana regen
                }
            }

            int dirtyMask = stats instanceof PlayerStats internal ? internal.consumeDirtyMask() : (stats.consumeDirty() ? com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.allMask() : 0);
            if (dirtyMask != 0) {
                RagnarDebugLog.playerData(
                        "SYNC source=tick player={} mask={} derived={} baseLv={} jobLv={} mana={}/{} sp={}/{} weight={}/{} over50={} over90={}",
                        p.getGameProfile().getName(),
                        com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.describeMask(dirtyMask),
                        com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.requiresDerivedSync(dirtyMask),
                        stats.getLevel(),
                        stats.getJobLevel(),
                        RagnarDebugLog.formatDouble(stats.getMana()),
                        RagnarDebugLog.formatDouble(stats.getManaMax()),
                        RagnarDebugLog.formatDouble(stats.getSP()),
                        RagnarDebugLog.formatDouble(stats.getSPMax()),
                        currentWeight,
                        maxWeight,
                        over50,
                        over90);
                Network.sendToPlayer(p, new PlayerStatsSyncPacket(stats, dirtyMask));
                if (com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.requiresDerivedSync(dirtyMask)) {
                    Network.sendToPlayer(p, new DerivedStatsSyncPacket(d));
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof Player p))
            return;
        var tgt = e.getEntity();

        if (tgt.level().isClientSide())
            return;

        if (DamageProcessingGuard.isProcessedPlayer(tgt)) {
            return;
        }

        RagnarCoreAPI.get(p).ifPresent(stats -> {
            double vit = StatAttributes.getTotal(p, StatKeys.VIT);
            double intel = StatAttributes.getTotal(p, StatKeys.INT);
            double attackerDex = StatAttributes.getTotal(p, StatKeys.DEX);
            double attackerLuk = StatAttributes.getTotal(p, StatKeys.LUK);

            double modifier = 1.0;

            double armaBase = getWeaponDamage(p);
            double aps = getWeaponAPS(p);
            double spellBase = getWeaponMagicDamage(p);
            double attackerArmorEff = getArmorEff(p);
            double defenderArmorEff = getArmorEff(tgt);
            double baseCast = 1.0;

            var d = StatComputer.compute(p, stats, armaBase, aps, spellBase, attackerArmorEff, baseCast);

            // Fetch Target Stats for proper HIT vs FLEE and Critical Shield
            double defenderFlee = 100.0;
            double defenderAgi = 0.0;
            double defenderDex = 1.0;
            double defenderLuk = 0.0;
            int defenderLevel = 1;
            double defenderVit = 0.0;
            double defenderInt = 0.0;
            double defenderMdefEquip = defenderArmorEff;
            double defenderPerfectDodge = 0.0;

            if (tgt instanceof Player tgtPlayer) {
                var tgtStatsOpt = RagnarCoreAPI.get(tgtPlayer);
                if (tgtStatsOpt.isPresent()) {
                    var ts = tgtStatsOpt.get();
                    int dodgeLv = com.etema.ragnarmmo.skill.data.progression.SkillProgressManager.getProgress(tgtPlayer, new ResourceLocation("ragnarmmo", "improve_dodge")).getLevel();
                    int defenderAgiTotal = (int) Math.round(StatAttributes.getTotal(tgtPlayer, StatKeys.AGI));
                    int defenderDexTotal = (int) Math.round(StatAttributes.getTotal(tgtPlayer, StatKeys.DEX));
                    int defenderLukTotal = (int) Math.round(StatAttributes.getTotal(tgtPlayer, StatKeys.LUK));
                    int defenderVitTotal = (int) Math.round(StatAttributes.getTotal(tgtPlayer, StatKeys.VIT));
                    int defenderIntTotal = (int) Math.round(StatAttributes.getTotal(tgtPlayer, StatKeys.INT));
                    defenderAgi = defenderAgiTotal;
                    defenderDex = defenderDexTotal;
                    defenderFlee = CombatMath.computeFLEE(defenderAgiTotal, defenderLukTotal, ts.getLevel(), dodgeLv * 3.0);
                    defenderLuk = defenderLukTotal;
                    defenderLevel = ts.getLevel();
                    defenderVit = defenderVitTotal;
                    defenderInt = defenderIntTotal;
                    defenderPerfectDodge = CombatMath.computePerfectDodge(defenderLukTotal);
                }
            } else if (tgt instanceof net.minecraft.world.entity.Mob mob) {
                var mobStatsLazy = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(mob);
                if (mobStatsLazy.isPresent()) {
                    var ms = mobStatsLazy.orElseThrow(() -> new IllegalStateException("MobStats absent after isPresent()"));
                    defenderAgi = ms.get(StatKeys.AGI);
                    defenderDex = ms.get(StatKeys.DEX);
                    defenderFlee = CombatMath.computeFLEE(ms.get(StatKeys.AGI), ms.get(StatKeys.LUK), ms.getLevel(), 0);
                    defenderLuk = ms.get(StatKeys.LUK);
                    defenderLevel = ms.getLevel();
                    defenderVit = ms.get(StatKeys.VIT);
                    defenderInt = ms.get(StatKeys.INT);
                } else {
                    defenderLevel = mob.getAttributes().hasAttribute(Attributes.MAX_HEALTH) ? (int) (mob.getMaxHealth() / 10) : 10;
                    defenderFlee = defenderLevel + CombatMath.FLEE_BASE;
                    defenderLuk = defenderLevel;
                }
            }

            boolean isMagic = isMagicDamage(e.getSource());
            var attackElement = isMagic
                    ? CombatPropertyResolver.getMagicElement(e.getSource().getDirectEntity())
                    : CombatPropertyResolver.getOffensiveElement(p);
            var defenseElement = CombatPropertyResolver.getDefensiveElement(tgt);
            modifier *= CombatPropertyResolver.getElementalModifier(attackElement, defenseElement);
            modifier *= EquipmentCombatModifierResolver.getOutgoingModifier(p, tgt, attackElement, isMagic);

            if (!isMagic && defenderPerfectDodge > 0.0
                    && ThreadLocalRandom.current().nextDouble() < defenderPerfectDodge) {
                RagnarDebugLog.combat(
                        "ATTACK result=PERFECT_DODGE attacker={} target={} type={} hit={} flee={} pDodge={} crit={} atkElem={} defElem={}",
                        RagnarDebugLog.entityLabel(p),
                        RagnarDebugLog.entityLabel(tgt),
                        isMagic ? "magic" : "physical",
                        RagnarDebugLog.formatDouble(d.accuracy),
                        RagnarDebugLog.formatDouble(defenderFlee),
                        RagnarDebugLog.percent(defenderPerfectDodge),
                        RagnarDebugLog.percent(d.criticalChance),
                        attackElement,
                        defenseElement);
                e.setCanceled(true);
                spawnMissParticles(tgt);
                DamageProcessingGuard.markProcessedPlayer(tgt);
                return;
            }

            // --- HIT VS FLEE Check ---
            double finalHitChance = CombatMath.computeHitRate(d.accuracy, defenderFlee);
            if (ThreadLocalRandom.current().nextDouble() > finalHitChance) {
                RagnarDebugLog.combat(
                        "ATTACK result=MISS attacker={} target={} type={} hit={} flee={} chance={} crit={} atkElem={} defElem={}",
                        RagnarDebugLog.entityLabel(p),
                        RagnarDebugLog.entityLabel(tgt),
                        isMagic ? "magic" : "physical",
                        RagnarDebugLog.formatDouble(d.accuracy),
                        RagnarDebugLog.formatDouble(defenderFlee),
                        RagnarDebugLog.percent(finalHitChance),
                        RagnarDebugLog.percent(d.criticalChance),
                        attackElement,
                        defenseElement);
                e.setCanceled(true);
                spawnMissParticles(tgt);
                DamageProcessingGuard.markProcessedPlayer(tgt);
                return;
            }

            double dmg = isMagic ? d.magicAttack : d.physicalAttack;
            double minPhysicalDamage = d.physicalAttackMin;

            // --- CRITICAL HIT SCALING & SHIELD ---
            double critShield = Math.floor(defenderLevel / 15.0) + Math.floor(defenderLuk / 5.0);
            double finalCritChance = Math.max(0.0, d.criticalChance - (critShield / 100.0));
            boolean criticalHit = false;

            if (!isMagic && ThreadLocalRandom.current().nextDouble() < finalCritChance) {
                dmg *= d.criticalDamageMultiplier;
                criticalHit = true;
            }

            double variance = 1.0;
            if (p.getTags().contains("ragnarmmo_maximize_power")) {
                variance = 1.0;
            } else if (!isMagic) {
                dmg = CombatMath.computeDamageVariance(dmg, (int) attackerDex, (int) attackerLuk,
                        ThreadLocalRandom.current());
            }

            // --- Size Penalty ---
            if (!isMagic && !CombatPropertyResolver.hasWeaponPerfection(p)) {
                CombatMath.MobSize targetSize = getMobSize(e.getEntity());
                double sizePenalty = CombatMath.getWeaponSizePenalty(p.getMainHandItem(), targetSize);
                dmg *= sizePenalty;
                minPhysicalDamage *= sizePenalty;
            }

            // --- Enchant Poison Check ---
            int epLv = p.getPersistentData()
                    .getInt(com.etema.ragnarmmo.skill.job.assassin.EnchantPoisonSkillEffect.ENCHANT_POISON_LEVEL_TAG);
            long epUntil = p.getPersistentData()
                    .getLong(com.etema.ragnarmmo.skill.job.assassin.EnchantPoisonSkillEffect.ENCHANT_POISON_UNTIL_TAG);
            if (epLv > 0 && epUntil > 0 && p.level().getGameTime() >= epUntil) {
                p.getPersistentData().remove(
                        com.etema.ragnarmmo.skill.job.assassin.EnchantPoisonSkillEffect.ENCHANT_POISON_LEVEL_TAG);
                p.getPersistentData().remove(
                        com.etema.ragnarmmo.skill.job.assassin.EnchantPoisonSkillEffect.ENCHANT_POISON_UNTIL_TAG);
                epLv = 0;
            }
            if (epLv > 0 && !isMagic) {
                if (p.level().random.nextFloat() < (0.05f + epLv * 0.03f)) {
                    e.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 100, 0));
                }
            }

            double afterSoft;
            if (isMagic) {
                double totalMdef = CombatMath.computeMDEF((int) defenderInt, (int) defenderVit, (int) defenderDex,
                        defenderLevel, defenderMdefEquip);
                double drMagic = CombatMath.computeMagicDR(totalMdef);
                afterSoft = CombatMath.applyMagicDefense(dmg, (int) defenderInt, drMagic);
            } else {
                double softDEF = CombatMath.computeSoftDEF((int) defenderVit, (int) defenderAgi, defenderLevel);
                double hardDEF = CombatMath.computeHardDEF(defenderArmorEff, (int) defenderVit);
                double drPhys = CombatMath.computePhysDR(hardDEF);
                afterSoft = CombatMath.applyPhysicalDefense(dmg, softDEF, hardDEF, drPhys);
                minPhysicalDamage = CombatMath.applyPhysicalDefense(minPhysicalDamage, softDEF, hardDEF, drPhys);
            }

            // Apply Tags (Element/Race/Size multiplier)
            afterSoft *= modifier;
            if (!isMagic) {
                minPhysicalDamage *= modifier;
            }

            // --- Frozen Status (Water Property Shift) ---
            if (tgt.hasEffect(com.etema.ragnarmmo.common.init.RagnarMobEffects.FROZEN.get())) {
                Entity direct = e.getSource().getDirectEntity();
                // Wind damage vs Frozen (Water)
                if (direct instanceof com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile) {
                    afterSoft *= 1.75;
                } 
                // Fire damage vs Frozen (Water)
                else if (direct instanceof com.etema.ragnarmmo.entity.projectile.FireBoltProjectile || 
                         direct instanceof com.etema.ragnarmmo.entity.projectile.FireBallProjectile ||
                         direct instanceof com.etema.ragnarmmo.entity.aoe.FireWallAoe) {
                    afterSoft *= 0.4;
                    // Fire breaks Frozen status in RO
                    tgt.removeEffect(com.etema.ragnarmmo.common.init.RagnarMobEffects.FROZEN.get());
                }
            }

            if (tgt.getTags().contains("ragnarmmo_lex_aeterna")) {
                afterSoft *= 2.0;
                if (!isMagic) {
                    minPhysicalDamage *= 2.0;
                }
                tgt.removeTag("ragnarmmo_lex_aeterna");
                tgt.setGlowingTag(false);
                if (tgt.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLASH, tgt.getX(), tgt.getY() + 1.0, tgt.getZ(), 1, 0, 0, 0, 0);
                }
            }

            if (!isMagic) {
                afterSoft = Math.max(afterSoft, minPhysicalDamage);
            }

            RagnarDebugLog.combat(
                    "ATTACK result={} attacker={} target={} type={} raw={} final={} minFinal={} hitChance={} critChance={} modifier={} atkElem={} defElem={} variance={} targetLvl={} targetVit={} targetInt={} size={}",
                    criticalHit ? "CRIT" : "HIT",
                    RagnarDebugLog.entityLabel(p),
                    RagnarDebugLog.livingState(tgt),
                    isMagic ? "magic" : "physical",
                    RagnarDebugLog.formatDouble(dmg),
                    RagnarDebugLog.formatDouble(afterSoft),
                    RagnarDebugLog.formatDouble(minPhysicalDamage),
                    RagnarDebugLog.percent(finalHitChance),
                    RagnarDebugLog.percent(finalCritChance),
                    RagnarDebugLog.formatDouble(modifier),
                    attackElement,
                    defenseElement,
                    RagnarDebugLog.formatDouble(variance),
                    defenderLevel,
                    RagnarDebugLog.formatDouble(defenderVit),
                    RagnarDebugLog.formatDouble(defenderInt),
                    getMobSize(e.getEntity()));
            e.setAmount((float) afterSoft);
            DamageProcessingGuard.markProcessedPlayer(tgt);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p))
            return;
        if (p.level().isClientSide())
            return;

        if (e.getSource().getEntity() instanceof Player)
            return;

        if (DamageProcessingGuard.isProcessedPlayer(p))
            return;

        boolean isMagic = isMagicDamage(e.getSource());
        double rawDamage = e.getAmount();
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity living ? living : null;

        RagnarCoreAPI.get(p).ifPresent(stats -> {
            double vit = StatAttributes.getTotal(p, StatKeys.VIT);
            double agi = StatAttributes.getTotal(p, StatKeys.AGI);
            double intel = StatAttributes.getTotal(p, StatKeys.INT);
            double dex = StatAttributes.getTotal(p, StatKeys.DEX);
            double luk = StatAttributes.getTotal(p, StatKeys.LUK);
            int level = stats.getLevel();

            // 1. FLEE Check (Only physical)
            if (!isMagic) {
                double playerPerfectDodge = CombatMath.computePerfectDodge((int) luk);
                if (ThreadLocalRandom.current().nextDouble() < playerPerfectDodge) {
                    RagnarDebugLog.combat(
                            "DEFEND result=PERFECT_DODGE defender={} attacker={} type={} pDodge={} raw={}",
                            RagnarDebugLog.entityLabel(p),
                            RagnarDebugLog.entityLabel(attacker),
                            isMagic ? "magic" : "physical",
                            RagnarDebugLog.percent(playerPerfectDodge),
                            RagnarDebugLog.formatDouble(rawDamage));
                    e.setCanceled(true);
                    spawnMissParticles(p);
                    DamageProcessingGuard.markProcessedPlayer(p);
                    return;
                }

                int dodgeLv = com.etema.ragnarmmo.skill.data.progression.SkillProgressManager.getProgress(p, new ResourceLocation("ragnarmmo", "improve_dodge")).getLevel();
                double playerFlee = CombatMath.computeFLEE((int) agi, (int) luk, level, dodgeLv * 3.0);
                double attackerHit = CombatMath.HIT_BASE; 
                Entity src = e.getSource().getEntity();
                if (src instanceof net.minecraft.world.entity.Mob mob) {
                    var attackerStatsOpt = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(mob);
                    if (attackerStatsOpt.isPresent()) {
                        com.etema.ragnarmmo.system.mobstats.core.MobStats as = attackerStatsOpt.orElseThrow(() -> new IllegalStateException("MobStats absent after isPresent()"));
                        attackerHit = CombatMath.computeHIT(as.get(StatKeys.DEX), as.get(StatKeys.LUK), as.getLevel(), 0);
                    } else {
                        int mobLevel = mob.getAttributes().hasAttribute(Attributes.MAX_HEALTH) ? (int) (mob.getMaxHealth() / 10) : 10;
                        attackerHit = CombatMath.HIT_BASE + (mobLevel * 1.5);
                    }
                }

                double finalHitChance = CombatMath.computeHitRate(attackerHit, playerFlee);
                if (ThreadLocalRandom.current().nextDouble() > finalHitChance) {
                    RagnarDebugLog.combat(
                            "DEFEND result=MISS defender={} attacker={} type={} attackerHit={} playerFlee={} chance={} raw={}",
                            RagnarDebugLog.entityLabel(p),
                            RagnarDebugLog.entityLabel(attacker),
                            isMagic ? "magic" : "physical",
                            RagnarDebugLog.formatDouble(attackerHit),
                            RagnarDebugLog.formatDouble(playerFlee),
                            RagnarDebugLog.percent(finalHitChance),
                            RagnarDebugLog.formatDouble(rawDamage));
                    e.setCanceled(true);
                    spawnMissParticles(p);
                    DamageProcessingGuard.markProcessedPlayer(p);
                    return;
                }
            }

            // 2. DEF Mitigación
            double afterSoft;
            double armorEff = getArmorEff(p);

            if (isMagic) {
                double mdef = CombatMath.computeMDEF((int) intel, (int) vit, (int) dex, level, armorEff);
                double drMagic = CombatMath.computeMagicDR(mdef);
                afterSoft = CombatMath.applyMagicDefense(rawDamage, (int) intel, drMagic);
            } else {

                double softDEF = CombatMath.computeSoftDEF((int) vit, (int) agi, level);
                double hardDEF = CombatMath.computeHardDEF(armorEff, (int) vit);
                double drPhys = CombatMath.computePhysDR(hardDEF);
                afterSoft = CombatMath.applyPhysicalDefense(rawDamage, softDEF, hardDEF, drPhys);
            }

            ElementType attackElement = resolveIncomingAttackElement(attacker, e.getSource().getDirectEntity(), isMagic);
            ElementType defenseElement = CombatPropertyResolver.getDefensiveElement(p);
            afterSoft *= CombatPropertyResolver.getElementalModifier(attackElement, defenseElement);
            afterSoft *= EquipmentCombatModifierResolver.getIncomingModifier(p, attacker, attackElement, isMagic);

            RagnarDebugLog.combat(
                    "DEFEND result=HIT defender={} attacker={} type={} raw={} final={} atkElem={} defElem={} vit={} agi={} int={} dex={} luk={}",
                    RagnarDebugLog.livingState(p),
                    RagnarDebugLog.entityLabel(attacker),
                    isMagic ? "magic" : "physical",
                    RagnarDebugLog.formatDouble(rawDamage),
                    RagnarDebugLog.formatDouble(Math.max(1.0, afterSoft)),
                    attackElement,
                    defenseElement,
                    RagnarDebugLog.formatDouble(vit),
                    RagnarDebugLog.formatDouble(agi),
                    RagnarDebugLog.formatDouble(intel),
                    RagnarDebugLog.formatDouble(dex),
                    RagnarDebugLog.formatDouble(luk));
            e.setAmount((float) Math.max(1.0, afterSoft));
            DamageProcessingGuard.markProcessedPlayer(p);
        });
    }

    @SubscribeEvent
    public static void onEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p))
            return;

        var instance = e.getEffectInstance();
        if (instance == null)
            return;

        var effect = instance.getEffect();
        if (effect != null) {
            var cat = effect.getCategory();
            if (cat == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                RagnarCoreAPI.get(p).ifPresent(stats -> {
                    int vit = stats.getVIT();
                    int intel = stats.getINT();
                    int luk = stats.getLUK();

                    String effectName = effect.getDescriptionId();
                    double resChance = 0.0;

                    if (effectName.contains("poison") || effectName.contains("wither") || effectName.contains("nausea")
                            || effectName.contains("weakness")) {
                        resChance = (vit * 0.5) + (luk * 0.2);
                    } else if (effectName.contains("blindness") || effectName.contains("slowness")
                            || effectName.contains("levitation") || effectName.contains("fatigue")
                            || effectName.contains("darkness")) {
                        resChance = (intel * 0.5) + (luk * 0.2);
                    } else {
                        resChance = (vit * 0.25) + (intel * 0.25) + (luk * 0.2); // Generic harmful effect
                    }

                    if (ThreadLocalRandom.current().nextDouble() * 100.0 < resChance) {
                        e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                        p.sendSystemMessage(Component.translatable("message.ragnarmmo.status_resisted")
                                .withStyle(net.minecraft.ChatFormatting.AQUA));
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer deadPlayer) {
            RagnarCoreAPI.get(deadPlayer).ifPresent(stats -> {
                int baseLost = 0;
                int currentExp = stats.getExp();
                if (currentExp > 0) {
                    baseLost = ExpTable.computeDeathPenaltyLoss(currentExp,
                            RagnarConfigs.SERVER.progression.baseExpDeathPenaltyRate.get());
                    stats.setExp(Math.max(0, currentExp - baseLost));
                }

                int jobLost = 0;
                int currentJobExp = stats.getJobExp();
                if (currentJobExp > 0) {
                    jobLost = ExpTable.computeDeathPenaltyLoss(currentJobExp,
                            RagnarConfigs.SERVER.progression.jobExpDeathPenaltyRate.get());
                    stats.setJobExp(Math.max(0, currentJobExp - jobLost));
                }

                if (baseLost > 0 || jobLost > 0) {
                    RagnarDebugLog.playerData(
                            "DEATH_PENALTY player={} baseLost={} jobLost={} baseExpNow={} jobExpNow={}",
                            deadPlayer.getGameProfile().getName(),
                            baseLost,
                            jobLost,
                            stats.getExp(),
                            stats.getJobExp());
                    deadPlayer.sendSystemMessage(
                            Component.translatable("message.ragnarmmo.exp_loss_death_dual", baseLost, jobLost));
                }
            });
            return;
        }

        if (!(e.getSource().getEntity() instanceof ServerPlayer sp))
            return;

        if (!shouldGiveExp(e.getEntity()))
            return;

        RagnarCoreAPI.get(sp).ifPresent(s -> {
            int baseExp = computeKillExp(e.getEntity());

            int finalExp = baseExp;
            if (MOB_EXP_SCALE_WITH_PLAYER_LEVEL) {
                finalExp = applyLevelPenalty(baseExp, sp, e.getEntity(), s.getLevel());
            }

            // Apply party XP sharing - distributes to party members and returns killer's
            // share
            finalExp = PartyXpService.distributeKillXp(sp, finalExp, sp.getServer());

            if (s instanceof PlayerStats internal) {
                internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
            }
            int baseAward = ExpTable.applyBaseExpRate(finalExp);
            int jobAward = ExpTable.applyJobExpRate(finalExp);

            int gained = s.addExpAndProcessLevelUps(baseAward, RagnarConfigs.SERVER.progression.pointsPerLevel.get(),
                    ExpTable::expToNext);
            int jobGained = s.addJobExpAndProcessLevelUps(jobAward, ExpTable::jobExpToNext);
            RagnarDebugLog.playerData(
                    "KILL_XP killer={} target={} baseRaw={} baseFinal={} baseAward={} jobAward={} baseLv={} jobLv={} levelUps={} jobLevelUps={}",
                    sp.getGameProfile().getName(),
                    RagnarDebugLog.entityLabel(e.getEntity()),
                    baseExp,
                    finalExp,
                    baseAward,
                    jobAward,
                    s.getLevel(),
                    s.getJobLevel(),
                    gained,
                    jobGained);
            sp.sendSystemMessage(Component.translatable("message.ragnarmmo.exp_gain",
                    baseAward, e.getEntity().getDisplayName()));
            if (gained > 0) {
                sp.sendSystemMessage(Component.translatable("message.ragnarmmo.level_up", gained));
            }
            if (jobGained > 0) {
                sp.sendSystemMessage(Component.translatable("message.ragnarmmo.job_level_up", jobGained));
            }
            PlayerStatsSyncService.sync(sp, s);

            // Update party HUD for killer
            PartyXpService.updatePartyMemberHud(sp);
        });
    }

    /**
     * Determines if a kill should award experience.
     * Awarded for Monsters and most Animals, excluding Players.
     */
    private static boolean shouldGiveExp(LivingEntity entity) {
        if (entity instanceof Player) return false;
        
        // Hostile monsters and bosses only
        if (entity instanceof net.minecraft.world.entity.monster.Monster ||
            entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
            entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return true;
        }

        // Passive mobs (Animals, Ambient, NPCs) do not grant EXP
        return false;
    }

    private static int computeKillExp(LivingEntity ent) {
        double hp = ent.getMaxHealth();
        double baseExp = hp * MOB_EXP_HP_MULTIPLIER;

        double armor = ent.getArmorValue();
        double armorBonus = armor * MOB_EXP_ARMOR_BONUS;

        int totalExp = (int) Math.round(baseExp + armorBonus);

        return Math.max(MOB_EXP_MINIMUM, Math.min(MOB_EXP_MAXIMUM, totalExp));
    }

    /**
     * RO-style EXP multiplier based on level difference.
     * Mobs lower than player give reduced EXP, mobs higher give bonus.
     * MVP/Boss mobs are exempt from penalties (always 100%).
     */
    private static int applyLevelPenalty(int baseExp, ServerPlayer player, LivingEntity mob, int playerLevel) {
        int mobLevel = estimateMobLevel(mob);
        boolean isBoss = MobUtils.isMVPBoss(mob);

        // MVP/Boss monsters never get EXP penalty
        if (isBoss) {
            return baseExp;
        }

        int diff = mobLevel - playerLevel;
        double multiplier = getROExpMultiplier(diff);

        return Math.max(1, (int) Math.round(baseExp * multiplier));
    }

    /**
     * RO-style EXP multiplier table.
     * Positive diff means mob is higher level than player.
     */
    private static double getROExpMultiplier(int levelDiff) {
        // Mob is much higher than player: bonus EXP
        if (levelDiff >= 25)
            return 1.50;
        if (levelDiff >= 20)
            return 1.30;
        if (levelDiff >= 15)
            return 1.20;
        if (levelDiff >= 10)
            return 1.10;
        if (levelDiff >= 6)
            return 1.05;

        // Mob is within ±5 levels: full EXP
        if (levelDiff >= -5)
            return 1.00;

        // Mob is lower than player: reduced EXP
        if (levelDiff >= -10)
            return 0.90; // -6 to -10
        if (levelDiff >= -15)
            return 0.70; // -11 to -15
        if (levelDiff >= -20)
            return 0.50; // -16 to -20
        if (levelDiff >= -25)
            return 0.30; // -21 to -25
        if (levelDiff >= -30)
            return 0.10; // -26 to -30

        // Mob is 30+ levels below player: minimal EXP
        return 0.01;
    }


    private static int estimateMobLevel(LivingEntity mob) {
        // Try to get actual level from MobStats capability
        var mobStatsOpt = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(mob);
        var stats = mobStatsOpt.resolve().orElse(null);
        if (stats != null) {
            int level = stats.getLevel();
            if (level > 0) {
                return level;
            }
        }

        // Fallback: estimate from HP
        double hp = mob.getMaxHealth();
        return Math.max(1, (int) (hp / 10.0));
    }

    public static double getWeaponDamage(Player p) {
        double base = p.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        ItemStack main = p.getItemInHand(InteractionHand.MAIN_HAND);
        Multimap<Attribute, AttributeModifier> mods = main.getAttributeModifiers(EquipmentSlot.MAINHAND);

        double add = 0.0, multBase = 0.0, multTotal = 0.0;
        for (var e : mods.entries()) {
            if (e.getKey() != Attributes.ATTACK_DAMAGE)
                continue;
            var m = e.getValue();
            switch (m.getOperation()) {
                case ADDITION -> add += m.getAmount();
                case MULTIPLY_BASE -> multBase += m.getAmount();
                case MULTIPLY_TOTAL -> multTotal += m.getAmount();
            }
        }
        double withItem = (base * (1.0 + multBase) + add) * (1.0 + multTotal);
        float ench = EnchantmentHelper.getDamageBonus(main, MobType.UNDEFINED);
        return withItem + ench + RoRefineMath.getAttackBonus(main);
    }

    public static double getWeaponAPS(Player p) {
        return RagnarCoreAPI.get(p).map(stats -> {
            int agi = stats.getAGI();
            int dex = stats.getDEX();
            boolean hasShield = p.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                    || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
            return CombatMath.computeAPS(p.getMainHandItem(), hasShield, agi, dex, 0);
        }).orElse(1.6);
    }

    public static double getWeaponMagicDamage(Player p) {
        return WeaponStatHelper.getDisplayedMagicAttack(p.getMainHandItem());
    }
    public static double getArmorEff(LivingEntity ent) {
        double armorEff = ent.getArmorValue();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = ent.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            armorEff += RoRefineMath.getDefenseBonus(stack);
        }

        return armorEff;
    }

    /**
     * Determines if a damage source should be treated as magical in RO terms.
     * Uses DamageType tags for semantic correctness. Falls back to msgId only
     * for vanilla legacy sources. "thorns" is physical retaliation in RO, not magic.
     */
    private static boolean isMagicDamage(net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return true;
        }
        if (source.typeHolder().is(new net.minecraft.resources.ResourceLocation("ragnarmmo", "is_magic"))) {
            return true;
        }
        // Minimal fallback: exact match only, no substring matching
        String msgId = source.getMsgId();
        return msgId.equals("magic") || msgId.equals("indirectMagic");
    }

    @SubscribeEvent
    public static void onVisibility(net.minecraftforge.event.entity.living.LivingEvent.LivingVisibilityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY) &&
                p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
            var inst = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            if (inst != null && inst.getAmplifier() >= 3) {
                Entity observer = e.getLookingEntity();
                if (observer instanceof LivingEntity le) {
                    if (!MobUtils.isMVPBoss(le)) {
                        e.modifyVisibility(0.0);
                    }
                } else {
                    e.modifyVisibility(0.0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent e) {
        if (e.getSource().getEntity() instanceof Player p) {
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY) &&
                    p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
                var inst = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                if (inst != null && inst.getAmplifier() >= 3) {
                    p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
                    p.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent e) {
        var target = e.getNewTarget();
        if (target instanceof Player p) {
            if (p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY) &&
                    p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)) {
                var inst = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                if (inst != null && inst.getAmplifier() >= 3) {
                    if (!MobUtils.isMVPBoss(e.getEntity())) {
                        e.setCanceled(true);
                    }
                }
            }
        }
    }

    private static void spawnMissParticles(LivingEntity ent) {
        Vec3 pos = ent.position();
        ent.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                pos.x, pos.y + ent.getBbHeight() * 0.8, pos.z, 0, 0.01, 0);
    }

    private static CombatMath.MobSize getMobSize(LivingEntity entity) {
        return CombatPropertyResolver.getEntitySize(entity);
    }

    private static ElementType resolveIncomingAttackElement(LivingEntity attacker, Entity directEntity, boolean isMagic) {
        if (isMagic) {
            return CombatPropertyResolver.getMagicElement(directEntity);
        }
        if (attacker instanceof Player player) {
            return CombatPropertyResolver.getOffensiveElement(player);
        }
        if (attacker != null) {
            return CombatPropertyResolver.getDefensiveElement(attacker);
        }
        return ElementType.NEUTRAL;
    }
}
