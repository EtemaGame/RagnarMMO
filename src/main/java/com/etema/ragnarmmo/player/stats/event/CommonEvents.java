package com.etema.ragnarmmo.player.stats.event;

import java.util.concurrent.ThreadLocalRandom;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadView;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.items.runtime.EquipmentCombatModifierResolver;
import com.etema.ragnarmmo.items.runtime.RoRefineMath;
import com.etema.ragnarmmo.items.runtime.RangedWeaponStatsHelper;
import com.etema.ragnarmmo.items.runtime.WeaponStatHelper;
import com.etema.ragnarmmo.player.stats.PlayerStatsModule;
import com.etema.ragnarmmo.player.stats.capability.PlayerStats;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
import com.etema.ragnarmmo.player.stats.compute.EquipmentStatSnapshot;
import com.etema.ragnarmmo.player.stats.compute.StatComputer;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.player.stats.network.DerivedStatsSyncPacket;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncPacket;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService;
import com.etema.ragnarmmo.player.progression.PlayerProgressionService;
import com.etema.ragnarmmo.player.stats.progression.JobBonusService;
import com.etema.ragnarmmo.player.party.PartyXpService;
import com.etema.ragnarmmo.mobs.util.MobUtils;
import com.etema.ragnarmmo.player.stats.util.AntiFarmManager;
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

@Mod.EventBusSubscriber(modid = PlayerStatsModule.MOD_ID)
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
                if (s instanceof PlayerStats internal) {
                    internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
                }
                JobBonusService.recomputeStats(sp, s);
                var derived = StatComputer.compute(sp, s, EquipmentStatSnapshot.capture(sp));
                if (s instanceof PlayerStats internal) {
                    internal.setSPMaxClient(derived.maxSP);
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

                // Give Money Bag if missing
                if (!sp.getInventory().contains(new net.minecraft.world.item.ItemStack(com.etema.ragnarmmo.items.ZenyItems.MONEY_BAG.get()))) {
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.etema.ragnarmmo.items.ZenyItems.MONEY_BAG.get()));
                }

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
            var d = StatComputer.compute(p, stats, EquipmentStatSnapshot.capture(p));

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

            if (Math.abs(previousSPMax - totalSpMax) > 1e-4) {
                stats.setSPMaxClient(totalSpMax);
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
                stats.addSP(d.spRegenPerSecond / 20.0);
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
                PlayerStatsSyncService.sync(p, stats, dirtyMask);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onHurt(LivingHurtEvent e) {
        var tgt = e.getEntity();
        if (tgt.level().isClientSide()) return;

        Entity direct = e.getSource().getDirectEntity();

        // --- PRIORITY RANGED BRANCH ---
        if (direct instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            net.minecraft.nbt.CompoundTag snapshotTag = arrow.getPersistentData()
                    .getCompound(RangedWeaponStatsHelper.SNAPSHOT_TAG);
            if (isValidBowSnapshot(arrow, snapshotTag)) {
                processRangedDamage(e, snapshotTag, arrow.getOwner(), tgt);
                return; // EXIT EARLY
            }
            // Arrow + invalid or missing snapshot -> return immediately without custom handling.
            return;
        }

        if (!(e.getSource().getEntity() instanceof Player p))
            return;

        // --- GENERIC MELEE PATH ---
        if (DamageProcessingGuard.isProcessedPlayer(tgt)) return;
        RagnarCoreAPI.get(p).ifPresent(stats -> {
            var dmgCalc = new com.etema.ragnarmmo.combat.engine.RagnarDamageCalculator();

            int dex = (int) StatAttributes.getTotal(p, StatKeys.DEX);
            int luk = (int) StatAttributes.getTotal(p, StatKeys.LUK);
            int str = (int) StatAttributes.getTotal(p, StatKeys.STR);
            int lvl = stats.getLevel();
            
            double weaponBaseAtk = getWeaponDamage(p);
            // Non-ranged hits (like hand punching with a bow) use STR-centric scaling
            double totalBaseAtk = CombatMath.computeTotalATK(str, dex, luk, lvl, weaponBaseAtk, 0, false);
            
            double dmg = dmgCalc.computePhysicalDamage(totalBaseAtk, dex, luk, ThreadLocalRandom.current());
            
            var attackElement = CombatPropertyResolver.getOffensiveElement(p);
            dmg = dmgCalc.applyModifiers(dmg, p.getMainHandItem(), tgt, attackElement, false);
            
            e.setAmount((float) Math.max(1.0, dmg));
            DamageProcessingGuard.markProcessedPlayer(tgt);
        });
    }

    private static void processRangedDamage(LivingHurtEvent e, net.minecraft.nbt.CompoundTag snapshot, Entity shooter, LivingEntity target) {
        double drawRatio = snapshot.contains("draw_ratio") ? snapshot.getDouble("draw_ratio") : 1.0D;
        double skillDamageMultiplier = snapshot.contains("skill_damage_multiplier")
                ? snapshot.getDouble("skill_damage_multiplier")
                : 1.0D;
        if (skillDamageMultiplier <= 0.0D) {
            skillDamageMultiplier = 1.0D;
        }
        double atk;
        if (RangedWeaponStatsHelper.DAMAGE_MODE_ATK_OVERRIDE.equals(snapshot.getString("damage_mode"))
                && snapshot.getDouble("atk_override") > 0.0D) {
            atk = snapshot.getDouble("atk_override");
        } else {
            atk = snapshot.getDouble("atk")
                    * Math.max(0.1D, Math.min(1.0D, drawRatio))
                    * skillDamageMultiplier;
        }
        double critChance = snapshot.getDouble("crit_chance");
        double critMult = snapshot.getDouble("crit_damage");
        int dex = snapshot.getInt("dex");
        int luk = snapshot.getInt("luk");
        com.etema.ragnarmmo.combat.element.ElementType element = com.etema.ragnarmmo.combat.element.ElementType.valueOf(snapshot.getString("element"));

        java.util.Random rng = new java.util.Random();
        var ts = CombatMath.getTargetStats(target);
        double armorEff = getArmorEff(target);

        double dmg = CombatMath.computeDamageVariance(atk, dex, luk, rng);
        if (rng.nextDouble() < critChance) {
            dmg *= Math.max(1.0D, critMult);
        }

        dmg *= CombatMath.getWeaponSizePenalty(new ItemStack(net.minecraft.world.item.Items.BOW), getMobSize(target));

        double hardDef = CombatMath.computeHardDEF(armorEff, ts.vit);
        double softDef = CombatMath.computeSoftDEF(ts.vit, ts.agi, 0);
        dmg = CombatMath.applyPhysicalDefense(dmg, softDef, hardDef, CombatMath.computePhysDR(hardDef));
        
        dmg *= CombatPropertyResolver.getElementalModifier(element, CombatPropertyResolver.getDefensiveElement(target));
        
        e.setAmount((float) Math.max(1.0, dmg));
        DamageProcessingGuard.markProcessedPlayer(target);
    }

    private static boolean isValidBowSnapshot(net.minecraft.world.entity.projectile.AbstractArrow arrow,
            net.minecraft.nbt.CompoundTag snapshotTag) {
        if (!snapshotTag.contains("version") || snapshotTag.getInt("version") < 1) {
            return false;
        }
        if (!"bow".equals(snapshotTag.getString("family")) || !snapshotTag.hasUUID("shooter_uuid")) {
            return false;
        }
        Entity owner = arrow.getOwner();
        return owner != null && snapshotTag.getUUID("shooter_uuid").equals(owner.getUUID());
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
                        // Cooldown to prevent spam
                        long now = System.currentTimeMillis();
                        long lastMsg = p.getPersistentData().getLong("ragnarmmo_last_res_msg");
                        if (now - lastMsg > 2000) { // 2 seconds cooldown
                            // Silenced specifically for slowness to prevent weight-system spam
                            if (!effectName.contains("slowness")) {
                                p.sendSystemMessage(Component.translatable("message.ragnarmmo.status_resisted")
                                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                                p.getPersistentData().putLong("ragnarmmo_last_res_msg", now);
                            }
                        }
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
                    baseLost = PlayerProgressionService
                            .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()))
                            .computeBaseDeathPenaltyLoss(currentExp);
                    stats.setExp(Math.max(0, currentExp - baseLost));
                }

                int jobLost = 0;
                int currentJobExp = stats.getJobExp();
                if (currentJobExp > 0) {
                    jobLost = PlayerProgressionService
                            .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()))
                            .computeJobDeathPenaltyLoss(currentJobExp);
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

        LivingEntity killed = e.getEntity();
        Entity sourceEntity = e.getSource().getEntity();
        ServerPlayer sp = null;

        if (sourceEntity instanceof ServerPlayer) {
            sp = (ServerPlayer) sourceEntity;
        } else {
            // Check for environmental kill credit (last hurt by)
            LivingEntity lastHurtBy = killed.getLastHurtByMob();
            if (lastHurtBy instanceof ServerPlayer) {
                // Only credit if the last hit was recent (e.g., within 5 seconds)
                if (killed.tickCount - killed.getLastHurtByMobTimestamp() < 100) {
                    sp = (ServerPlayer) lastHurtBy;
                }
            }
        }

        if (sp == null)
            return;

        if (!shouldGiveExp(killed))
            return;

        // Anti-Farm Check (Stationary Penalty)
        Vec3 lastPos = sp.getPersistentData().get("ragnarmmo_last_kill_pos") instanceof net.minecraft.nbt.DoubleTag 
            ? new Vec3(sp.getPersistentData().getDouble("ragnarmmo_last_kill_x"), 0, sp.getPersistentData().getDouble("ragnarmmo_last_kill_z"))
            : null;
        
        double distSq = lastPos != null ? sp.position().distanceToSqr(lastPos.x, sp.getY(), lastPos.z) : 100.0;
        int stationaryKills = sp.getPersistentData().getInt("ragnarmmo_stationary_kills");
        
        if (distSq < 25.0) { // 5 blocks radius
            stationaryKills++;
        } else {
            stationaryKills = 0;
        }
        
        sp.getPersistentData().putInt("ragnarmmo_stationary_kills", stationaryKills);
        sp.getPersistentData().putDouble("ragnarmmo_last_kill_x", sp.getX());
        sp.getPersistentData().putDouble("ragnarmmo_last_kill_z", sp.getZ());

        final int finalStationaryKills = stationaryKills;
        final ServerPlayer finalSp = sp;

        // Compute anti-farm penalty BEFORE the lambda (must be effectively final)
        double antiFarmPenaltyRaw = AntiFarmManager.getPenaltyFactor(sp);
        final double antiFarmPenalty = antiFarmPenaltyRaw;
        final boolean shouldWarnFarm = antiFarmPenalty < 1.0 && sp.tickCount % 600 == 0;

        RagnarCoreAPI.get(sp).ifPresent(s -> {
            int baseExp = computeKillExp(killed);
            
            // Apply expanded anti-farm penalty
            if (antiFarmPenalty < 1.0) {
                baseExp = (int)(baseExp * antiFarmPenalty);
                if (shouldWarnFarm) {
                    finalSp.sendSystemMessage(Component.translatable("message.ragnarmmo.anti_farm_warning").withStyle(net.minecraft.ChatFormatting.YELLOW));
                }
            }


            int finalExp = baseExp;
            if (MOB_EXP_SCALE_WITH_PLAYER_LEVEL) {
                finalExp = applyLevelPenalty(baseExp, finalSp, killed, s.getLevel());
            }

            // Apply party XP sharing - distributes to party members and returns killer's share
            finalExp = PartyXpService.distributeKillXp(finalSp, finalExp, finalSp.getServer());

            if (s instanceof PlayerStats internal) {
                internal.ensureBaseStatBaseline(RagnarConfigs.SERVER.progression.baseStatPoints.get());
            }
            PlayerProgressionService progressionService = PlayerProgressionService
                    .forJobId(net.minecraft.resources.ResourceLocation.tryParse(s.getJobId()));
            int baseAward = progressionService.applyBaseExpRate(finalExp);
            int jobAward = progressionService.applyJobExpRate(finalExp);

            int gained = s.addExpAndProcessLevelUps(baseAward, RagnarConfigs.SERVER.progression.pointsPerLevel.get(),
                    progressionService::baseExpToNext);
            int jobGained = s.addJobExpAndProcessLevelUps(jobAward, progressionService::jobExpToNext);
            RagnarDebugLog.playerData(
                    "KILL_XP killer={} target={} baseRaw={} baseFinal={} baseAward={} jobAward={} baseLv={} jobLv={} levelUps={} jobLevelUps={}",
                    finalSp.getGameProfile().getName(),
                    RagnarDebugLog.entityLabel(e.getEntity()),
                    baseExp,
                    finalExp,
                    baseAward,
                    jobAward,
                    s.getLevel(),
                    s.getJobLevel(),
                    gained,
                    jobGained);
            finalSp.sendSystemMessage(Component.translatable("message.ragnarmmo.exp_gain_dual",
                    baseAward, jobAward, e.getEntity().getDisplayName()));
            if (gained > 0) {
                finalSp.sendSystemMessage(Component.translatable("message.ragnarmmo.level_up", gained));
            }
            if (jobGained > 0) {
                finalSp.sendSystemMessage(Component.translatable("message.ragnarmmo.job_level_up", jobGained));
            }
            PlayerStatsSyncService.sync(finalSp, s);

            // Update party HUD for killer
            PartyXpService.updatePartyMemberHud(finalSp);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof Player player) {
            double penalty = AntiFarmManager.getPenaltyFactor(player);
            if (penalty < 1.0) {
                // Remove drops based on penalty
                // If penalty is 0.5, we keep 50% of drops
                event.getDrops().removeIf(drop -> player.getRandom().nextDouble() > penalty);
            }
        }
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
     * Boss-like mobs are exempt from penalties (always 100%).
     */
    private static int applyLevelPenalty(int baseExp, ServerPlayer player, LivingEntity mob, int playerLevel) {
        int mobLevel = estimateMobLevel(mob);
        boolean isBoss = MobUtils.isBossLike(mob);

        // Boss-like monsters never get EXP penalty
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
        int normalizedLevel = CombatMath.tryGetTargetLevel(mob).orElse(0);
        if (normalizedLevel > 0) {
            return normalizedLevel;
        }

        // If no resolved level exists yet, estimate from HP.
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
        MobConsumerReadView readView = ent instanceof Player ? null : MobConsumerReadViewResolver.resolve(ent).orElse(null);
        if (readView != null && readView.inspectionStats() != null) {
            return readView.inspectionStats().def();
        }

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
     * Extracts magical defense from armor items and refinements.
     * In Ragnarok, MDEF is typically granted by specific armors or refinements.
     */
    public static double getArmorMagicDefense(LivingEntity ent) {
        MobConsumerReadView readView = ent instanceof Player ? null : MobConsumerReadViewResolver.resolve(ent).orElse(null);
        if (readView != null && readView.inspectionStats() != null) {
            return readView.inspectionStats().mdef();
        }

        double equipMdef = 0.0;
        
        // Sum MDEF from armor attributes
        var attrInstance = ent.getAttribute(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.MAGIC_DEFENSE.get());
        if (attrInstance != null) {
            equipMdef += attrInstance.getValue();
        }

        // Add refinement bonuses to MDEF (custom mechanic for RagnarMMO)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = ent.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                // High refinements grant a small MDEF bonus
                int refine = com.etema.ragnarmmo.items.runtime.RoItemNbtHelper.getRefineLevel(stack);
                if (refine >= 5) {
                    equipMdef += (refine - 4); 
                }
            }
        }

        return equipMdef;
    }

    /**
     * Determines if a damage source should be treated as magical in RO terms.
     * Uses DamageType tags for semantic correctness. Falls back to msgId only
     * for vanilla damage sources. "thorns" is physical retaliation in RO, not magic.
     */
    public static boolean isMagicDamage(net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            return true;
        }
        if (source.typeHolder().is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ragnarmmo", "is_magic"))) {
            return true;
        }
        // Minimal exact-match path only, no substring matching
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
                    if (!MobUtils.isBossLike(le)) {
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
        if (!e.getEntity().level().isClientSide()
                && e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            net.minecraft.nbt.CompoundTag snapshotTag = arrow.getPersistentData()
                    .getCompound(RangedWeaponStatsHelper.SNAPSHOT_TAG);
            if (isValidBowSnapshot(arrow, snapshotTag) && snapshotTag.getBoolean("bypass_iframes")) {
                e.getEntity().invulnerableTime = 0;
            }
        }

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
                    if (!MobUtils.isBossLike(e.getEntity())) {
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

    public static ElementType resolveIncomingAttackElement(LivingEntity attacker, Entity directEntity, boolean isMagic) {
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
