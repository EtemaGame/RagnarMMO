package com.etema.ragnarmmo.system.stats.event;

import java.util.concurrent.ThreadLocalRandom;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.system.stats.RagnarStats;
import com.etema.ragnarmmo.system.stats.capability.PlayerStats;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import com.etema.ragnarmmo.system.stats.compute.StatComputer;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.stats.net.DerivedStatsSyncPacket;
import com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket;
import com.etema.ragnarmmo.system.stats.progression.ExpTable;
import com.etema.ragnarmmo.system.stats.progression.JobBonusService;
import com.etema.ragnarmmo.system.stats.party.PartyXpService;
import com.google.common.collect.Multimap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    private static final double MOB_EXP_HP_MULTIPLIER = 2.0;
    private static final double MOB_EXP_ARMOR_BONUS = 0.5;
    private static final int MOB_EXP_MINIMUM = 5;
    private static final int MOB_EXP_MAXIMUM = 10000;
    private static final boolean MOB_EXP_SCALE_WITH_PLAYER_LEVEL = true;

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            RagnarCoreAPI.get(sp).ifPresent(s -> {
                double armaBase = getWeaponDamage(sp);
                var derived = StatComputer.compute(sp, s, armaBase, getWeaponAPS(sp), 0, getArmorEff(sp), 1.0);
                if (s instanceof PlayerStats internal) {
                    internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
                }
                JobBonusService.recomputeStats(sp, s);
                s.setManaMaxClient(derived.maxMana);
                if (s instanceof PlayerStats internal) {
                    internal.setSPMaxClient(derived.maxMana); // Using derived.maxMana as base for SP too in join
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

                Network.sendToPlayer(sp, new PlayerStatsSyncPacket(s));
                Network.sendToPlayer(sp, new DerivedStatsSyncPacket(derived));
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
            var d = StatComputer.compute(p, stats, getWeaponDamage(p), getWeaponAPS(p), 0, getArmorEff(p), 1.0);

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

            var spAttr = p.getAttribute(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.MAX_SP.get());
            double totalSpMax = baseManaMax + (spAttr != null ? spAttr.getValue() : 0.0);

            double previousManaMax = stats.getManaMax();
            double previousSPMax = (stats instanceof PlayerStats internal) ? internal.getSPMax() : previousManaMax;

            if (Math.abs(previousManaMax - totalManaMax) > 1e-4 || Math.abs(previousSPMax - totalSpMax) > 1e-4) {
                stats.setManaMaxClient(totalManaMax);
                if (stats instanceof PlayerStats internal) {
                    internal.setSPMaxClient(totalSpMax);
                }
                stats.markDirty();
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
                    internal.addSP(d.manaRegenPerSecond / 20.0);
                }
            }

            if (stats.consumeDirty()) {
                Network.sendToPlayer(p, new PlayerStatsSyncPacket(stats));
                Network.sendToPlayer(p, new DerivedStatsSyncPacket(d));
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

            // Data-Driven Mob Modifiers
            double modifier = 1.0;
            if (tgt instanceof net.minecraft.world.entity.Mob mob) {
                var type = mob.getType();
                // We'll give flat examples here. A packmaker can overwrite these with actual
                // systems.
                if (type.is(com.etema.ragnarmmo.common.tags.RagnarTags.Entities.SIZE_SMALL))
                    modifier *= 1.25;
                if (type.is(com.etema.ragnarmmo.common.tags.RagnarTags.Entities.SIZE_LARGE))
                    modifier *= 0.75; // Typical sword penalty

                var weapon = p.getMainHandItem();
                if (type.is(com.etema.ragnarmmo.common.tags.RagnarTags.Entities.ELEMENT_FIRE) &&
                        weapon.is(com.etema.ragnarmmo.common.tags.RagnarTags.Items.ELEMENT_WATER)) {
                    modifier *= 1.5;
                }
            }

            double armaBase = getWeaponDamage(p);
            double aps = getWeaponAPS(p);
            double spellBase = 0;
            double armorEff = getArmorEff(tgt);
            double baseCast = 1.0;

            var d = StatComputer.compute(p, stats, armaBase, aps, spellBase, armorEff, baseCast);

            // Fetch Target Stats for proper HIT vs FLEE and Critical Shield
            double defenderFlee = 100.0;
            double defenderLuk = 0.0;
            int defenderLevel = 1;

            if (tgt instanceof Player tgtPlayer) {
                var tgtStatsOpt = RagnarCoreAPI.get(tgtPlayer);
                if (tgtStatsOpt.isPresent()) {
                    var ts = tgtStatsOpt.get();
                    defenderFlee = StatAttributes.getTotal(tgtPlayer, StatKeys.AGI) * CombatMath.AGI_TO_FLEE_MULT
                            + StatAttributes.getTotal(tgtPlayer, StatKeys.LUK) / CombatMath.LUK_TO_FLEE_DIVISOR
                            + ts.getLevel() * CombatMath.LEVEL_TO_FLEE_MULT + CombatMath.FLEE_BASE;
                    defenderLuk = StatAttributes.getTotal(tgtPlayer, StatKeys.LUK);
                    defenderLevel = ts.getLevel();
                }
            } else if (tgt instanceof net.minecraft.world.entity.Mob mob) {
                // Approximate monster stats
                defenderLevel = mob.getAttributes().hasAttribute(Attributes.MAX_HEALTH)
                        ? (int) (mob.getMaxHealth() / 10)
                        : 10;
                defenderFlee = defenderLevel + CombatMath.FLEE_BASE; // Balanced formula appx
                defenderLuk = defenderLevel;
            }

            // --- HIT VS FLEE Check ---
            // PERFECT HIT implementation check (equipment/cards) could go here later.
            double finalHitChance = CombatMath.computeHitRate(d.accuracy, defenderFlee);
            // Perfect dodge check vs player LUK is omitted for monsters, but could be added
            // easily.
            if (ThreadLocalRandom.current().nextDouble() > finalHitChance) {
                e.setCanceled(true);
                spawnMissParticles(tgt);
                DamageProcessingGuard.markProcessedPlayer(tgt);
                return;
            }

            boolean isMagic = isMagicDamage(e.getSource());
            double dmg = isMagic ? d.magicAttack : d.physicalAttack;

            // --- CRITICAL HIT SCALING & SHIELD ---
            // Critical Hit Shield: floor(EnemyBaseLevel / 15) + floor(EnemyLUK / 5)
            // It reduces the attacker's chance by a flat absolute percentage.
            double critShield = Math.floor(defenderLevel / 15.0) + Math.floor(defenderLuk / 5.0);
            double finalCritChance = Math.max(0.0, d.criticalChance - (critShield / 100.0));

            if (!isMagic && ThreadLocalRandom.current().nextDouble() < finalCritChance)
                dmg *= d.criticalDamageMultiplier;

            double afterSoft = isMagic ? Math.max(dmg - (intel * 0.3), dmg * 0.1)
                    : Math.max(dmg - (vit + Math.floor(vit / 5.0) * Math.floor(vit / 5.0) * 0.2), dmg * 0.1);

            // Apply Tags (Element/Race/Size multiplier)
            afterSoft *= modifier;

            double afterHard = afterSoft * (1 - (isMagic ? d.magicDamageReduction : d.physicalDamageReduction));

            if (!isMagic)
                afterHard = Math.max(afterHard, d.physicalAttackMin * modifier);

            e.setAmount((float) afterHard);
            DamageProcessingGuard.markProcessedPlayer(tgt);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p))
            return;
        if (p.level().isClientSide())
            return;

        // Skip if attacker is a player because onHurt (NORMAL) already handled PvP
        // fully
        if (e.getSource().getEntity() instanceof Player)
            return;

        if (DamageProcessingGuard.isProcessedPlayer(p))
            return;

        boolean isMagic = isMagicDamage(e.getSource());
        double rawDamage = e.getAmount();

        RagnarCoreAPI.get(p).ifPresent(stats -> {
            double vit = StatAttributes.getTotal(p, StatKeys.VIT);
            double agi = StatAttributes.getTotal(p, StatKeys.AGI);
            double intel = StatAttributes.getTotal(p, StatKeys.INT);
            double luk = StatAttributes.getTotal(p, StatKeys.LUK);
            int level = stats.getLevel();

            // 1. FLEE Check (Only physical)
            if (!isMagic) {
                double playerFlee = CombatMath.computeFLEE((int) agi, (int) luk, level, 0);
                double attackerHit = CombatMath.HIT_BASE; // Base hit
                Entity src = e.getSource().getEntity();
                if (src instanceof net.minecraft.world.entity.Mob mob) {
                    int mobLevel = mob.getAttributes().hasAttribute(Attributes.MAX_HEALTH)
                            ? (int) (mob.getMaxHealth() / 10)
                            : 10;
                    attackerHit = CombatMath.HIT_BASE + (mobLevel * 1.5); // Appx monster hit
                }

                double finalHitChance = CombatMath.computeHitRate(attackerHit, playerFlee);
                if (ThreadLocalRandom.current().nextDouble() > finalHitChance) {
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
                double mdef = CombatMath.computeMDEF((int) intel, (int) vit, 1, level, armorEff);
                double drMagic = CombatMath.computeMagicDR(mdef);
                afterSoft = CombatMath.applyMagicDefense(rawDamage, (int) intel, drMagic);
            } else {
                double softDEF = CombatMath.computeSoftDEF((int) vit, (int) agi, level);
                double hardDEF = CombatMath.computeHardDEF(armorEff, (int) vit);
                double drPhys = CombatMath.computePhysDR(hardDEF);
                afterSoft = CombatMath.applyPhysicalDefense(rawDamage, softDEF, hardDEF, drPhys);
            }

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
                int currentExp = stats.getExp();
                if (currentExp > 0) {
                    int basePenalty = (int) Math.round(currentExp * 0.05);
                    stats.setExp(Math.max(0, currentExp - basePenalty));
                }

                int currentJobExp = stats.getJobExp();
                if (currentJobExp > 0) {
                    int jobPenalty = (int) Math.round(currentJobExp * 0.05);
                    stats.setJobExp(Math.max(0, currentJobExp - jobPenalty));
                }

                deadPlayer.sendSystemMessage(Component.translatable("message.ragnarmmo.exp_loss_death"));
                stats.markDirty();
            });
            return;
        }

        if (!(e.getSource().getEntity() instanceof ServerPlayer sp))
            return;

        if (!isHostileMob(e.getEntity()))
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
            int gained = s.addExpAndProcessLevelUps(finalExp, RagnarConfigs.SERVER.progression.pointsPerLevel.get(),
                    ExpTable::expToNext);
            int jobGained = s.addJobExpAndProcessLevelUps(finalExp, ExpTable::jobExpToNext);
            s.markDirty();
            sp.sendSystemMessage(Component.translatable("message.ragnarmmo.exp_gain",
                    finalExp, e.getEntity().getDisplayName()));
            if (gained > 0) {
                sp.sendSystemMessage(Component.translatable("message.ragnarmmo.level_up", gained));
            }
            if (jobGained > 0) {
                sp.sendSystemMessage(Component.translatable("message.ragnarmmo.job_level_up", jobGained));
            }
            Network.sendToPlayer(sp, new PlayerStatsSyncPacket(s));
            // Sync derived stats after level/stat changes
            var derivedAfterKill = StatComputer.compute(sp, s, getWeaponDamage(sp), getWeaponAPS(sp), 0,
                    getArmorEff(sp), 1.0);
            Network.sendToPlayer(sp, new DerivedStatsSyncPacket(derivedAfterKill));

            // Update party HUD for killer
            PartyXpService.updatePartyMemberHud(sp);
        });
    }

    private static boolean isHostileMob(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Monster ||
                entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
                entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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
        boolean isBoss = isMVPBoss(mob);

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

    /**
     * Check if mob is an MVP/Boss and exempt from EXP penalties.
     */
    private static boolean isMVPBoss(LivingEntity mob) {
        // Check MobStats capability for tier
        var mobStatsOpt = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(mob);
        var stats = mobStatsOpt.resolve().orElse(null);
        if (stats != null) {
            var tier = stats.getTier();
            if (tier == com.etema.ragnarmmo.common.api.mobs.MobTier.BOSS) {
                return true;
            }
        }

        // Fallback: check vanilla boss types
        return mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
                mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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
        return withItem + ench;
    }

    private static double getWeaponAPS(Player p) {
        return RagnarCoreAPI.get(p).map(stats -> {
            int agi = stats.getAGI();
            int dex = stats.getDEX();
            boolean hasShield = p.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                    || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
            return CombatMath.computeAPS(p.getMainHandItem(), hasShield, agi, dex, 0);
        }).orElse(1.6);
    }

    private static double getArmorEff(LivingEntity ent) {
        return ent.getArmorValue();
    }

    private static boolean isMagicDamage(net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return true;
        }
        String msgId = source.getMsgId();
        return msgId.contains("magic") || msgId.contains("spell")
                || msgId.contains("indirectMagic") || msgId.contains("thorns");
    }

    private static void spawnMissParticles(LivingEntity ent) {
        Vec3 pos = ent.position();
        ent.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                pos.x, pos.y + ent.getBbHeight() * 0.8, pos.z, 0, 0.01, 0);
    }
}
