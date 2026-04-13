package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.api.mobs.MobScalingMode;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.system.mobstats.core.MobStatDistributor;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.system.mobstats.world.ActiveBossesSavedData;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnMetadata;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobStatsPacket;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.etema.ragnarmmo.system.stats.util.AntiFarmManager;
import com.etema.ragnarmmo.common.config.RagnarConfigs;

import java.util.Map;
import java.util.Random;
import com.etema.ragnarmmo.system.mobstats.ai.MobUseSkillGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.EnderMan;

/**
 * Legacy spawn-time stats and {@link MobTier} assignment handler.
 *
 * <p>This class still drives the old capability-based mob-stats pipeline. It coexists with the
 * new manual/datapack path, but it is not the semantic authority for migrated manual mobs and
 * should not be treated as the new architecture's primary encounter model.</p>
 */
public class MobSpawnHandler {

    private final Random rng = new Random();
    private final MobLevelManager levelMgr = new MobLevelManager(rng);
    private final MobStatDistributor distributor = new MobStatDistributor(rng);

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!RagnarConfigs.SERVER.progression.antiFarmSpawnReduction.get()) return;
        
        // Find nearest player to check for anti-farm penalty
        Player nearest = event.getLevel().getNearestPlayer(event.getX(), event.getY(), event.getZ(), 64, false);
        if (nearest != null) {
            double penalty = AntiFarmManager.getPenaltyFactor(nearest);
            if (penalty < 1.0) {
                if (rng.nextDouble() > penalty) {
                    event.setSpawnCancelled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player)
            return;

        // 1. Primary path: Try to initialize under the new strict/manual architecture
        boolean initializedNew = com.etema.ragnarmmo.system.mobstats.service.MobRuntimeInitializationService.tryInitialize(living);
        if (initializedNew) {
            com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore.get(living).ifPresent(profile -> {
                // Handle world-level persistence for bosses
                com.etema.ragnarmmo.system.mobstats.service.BossWorldRegistrationBridge.handleRegistration(living, profile.rank());
                
                // Add AI augmentation if it's a mob
                if (living instanceof Mob mob) {
                    com.etema.ragnarmmo.system.mobstats.service.MobAiAugmentationService.addMobClassAI(mob, assignInternalClass(mob));
                }
            });
            return; // COMPLETELY BYPASS LEGACY PIPELINE
        }

        // 2. Legacy fallback: Check if the legacy pipeline should still be bypassed anyway (STRICT mode guard)
        var effectiveMethod = MobLevelManager.resolveEffectiveMethod(living);
        if (effectiveMethod.method() == MobScalingMode.MANUAL
                && effectiveMethod.source() == MobLevelManager.DifficultyMethodSource.DATAPACK) {
            RagnarDebugLog.mobSpawns(
                    "Skipping legacy spawn pipeline for {} because it is marked as MANUAL/DATAPACK but resolution failed (Authority Guard).",
                    RagnarDebugLog.entityLabel(living));
            return;
        }

        // 3. Fallback path: Legacy capability-based stats
        MobStatsProvider.get(living).ifPresent(stats -> {
            if (stats.isInitialized()) {
                normalizeExistingNaturalCreeperBoss(living, stats);
                return;
            }

            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            MobCategory cat = living.getType().getCategory();

            if (mobId != null && MobStatsConfigAccess.isEnabled() && MobStatsConfigAccess.getMobExcludeList().contains(mobId.toString()))
                return;

            boolean isPassive = cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT
                    || cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT;
            generateStats(living, stats, isPassive);
            stats.setInitialized(true);

            applyAttributes(living, stats);

            if (stats.getTier().shouldPersistWorldState()) {
                if (BossSpawnMetadata.getSpawnKey(living).isEmpty()) {
                    BossSpawnMetadata.markNatural(living);
                }
                if (living instanceof Mob persistentMob) {
                    persistentMob.setPersistenceRequired();
                }
            }

            if (living.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && stats.getTier().shouldPersistWorldState()
                    && serverLevel.getServer() != null) {
                ActiveBossesSavedData.get(serverLevel.getServer()).registerBoss(serverLevel, living, stats.getTier());
            }

            RagnarDebugLog.mobSpawns(
                    "LEGACY SPAWN mob={} pos={} tier={} level={}",
                    RagnarDebugLog.entityLabel(living),
                    RagnarDebugLog.blockPos(living.blockPosition()),
                    stats.getTier(),
                    stats.getLevel());

            // Sincroniza con clientes (Legacy sync packet)
            SyncMobStatsPacket.fromEntity(living)
                    .ifPresent(packet -> Network.sendTrackingEntityAndSelf(living, packet));
            
            if (living instanceof Mob mob) {
                com.etema.ragnarmmo.system.mobstats.service.MobAiAugmentationService.addMobClassAI(mob, stats.getMobClass());
            }
        });
    }

    private static void addMobClassAI(LivingEntity entity, MobClass mobClass) {
        if (!(entity instanceof Mob mob)) return;
        
        switch (mobClass) {
            case SWORDMAN:
                // Bash: Range 3.5, CD 4s (80 ticks), Mana 15
                mob.goalSelector.addGoal(2, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "bash"), 3.5, 80, 15));
                break;
            case ACOLYTE:
                // Heal: Range 8.0, CD 5s (100 ticks), Mana 20
                mob.goalSelector.addGoal(3, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "heal"), 8.0, 100, 20));
                break;
            case MAGE:
                // Fire Bolt: Range 12.0, CD 3s (60 ticks), Mana 20
                mob.goalSelector.addGoal(2, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "fire_bolt"), 12.0, 60, 20));
                break;
            default:
                break;
        }
    }

    private void generateStats(LivingEntity mob, MobStats stats, boolean isPassive) {
        if (isPassive) {
            stats.setTier(MobTier.NORMAL);
            stats.setLevel(1);


            // Basic multipliers for passives (can be tweaked if needed, but 1.0 is
            // standard)
            stats.setHealthMultiplier(1.0);
            stats.setDamageMultiplier(1.0);
            stats.setDefenseMultiplier(1.0);
            stats.setSpeedMultiplier(1.0);

            // No attribute distribution for passives, they stay vanilla-ish but tracked
            return;
        }

        MobTier tier = MobSpawnOverrides.consumeForcedTier(mob)
                .map(this::normalizeNaturalTier)
                .orElseGet(() -> determineTier(mob));
        stats.setTier(tier);

        MobLevelManager.LevelResolution levelResolution = levelMgr.computeLevelResolution(mob, tier);
        int level = levelResolution.level();
        stats.setLevel(level);

        MobClass internalClass = assignInternalClass(mob);
        stats.setMobClass(internalClass);

        int base = MobStatsConfigAccess.getBasePoints(tier);
        Map<StatKeys, Integer> weights = new java.util.EnumMap<>(internalClass.getWeights());
        int ppl = MobStatsConfigAccess.getPointsPerLevel(tier);
        int total = Math.max(0, base + level * ppl);

        distributeWithCalculatedWeights(stats, total, weights);

        double hm = MobStatsConfigAccess.getHealthMultiplier(tier) * internalClass.getHpMult();
        double dm = MobStatsConfigAccess.getDamageMultiplier(tier) * internalClass.getDmgMult();
        double df = MobStatsConfigAccess.getDefenseMultiplier(tier) * internalClass.getDefMult();
        double sp = internalClass.getSpdMult();

        // PARTY SCALING
        if (tier != MobTier.NORMAL) {
            double radius = MobStatsConfigAccess.getPartyScalingRadius();
            long playerCount = mob.level().players().stream()
                    .filter(p -> p.distanceToSqr(mob) < radius * radius)
                    .count();

            if (playerCount > 1) {
                double extraPlayers = playerCount - 1;
                hm *= (1.0 + extraPlayers * MobStatsConfigAccess.getPartyHpMultiplier());
                dm *= (1.0 + extraPlayers * MobStatsConfigAccess.getPartyAtkMultiplier());
            }
        }

        stats.setHealthMultiplier(hm);
        stats.setDamageMultiplier(dm);
        stats.setDefenseMultiplier(df);
        stats.setSpeedMultiplier(sp);
    }

    private MobClass assignInternalClass(LivingEntity mob) {
        if (mob instanceof Zombie) return MobClass.SWORDMAN;
        if (mob instanceof Skeleton) return MobClass.ARCHER;
        if (mob instanceof Spider) return MobClass.THIEF;
        if (mob instanceof Creeper) return MobClass.THIEF;
        if (mob instanceof Blaze || mob instanceof Witch) return MobClass.MAGE;
        if (mob instanceof EnderMan) return MobClass.THIEF;
        
        // Default based on attributes
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance spd = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        
        if (atk != null && atk.getBaseValue() > 3) return MobClass.SWORDMAN;
        if (spd != null && spd.getBaseValue() > 0.3) return MobClass.THIEF;
        
        return MobClass.NOVICE;
    }

    private void distributeWithCalculatedWeights(MobStats stats, int totalPoints, Map<StatKeys, Integer> weights) {
        stats.resetStats();
        distributor.distributeByWeights(stats, totalPoints, weights);
    }

    /**
     * Legacy {@link MobTier} selection for the old spawn pipeline.
     *
     * <p>This remains compatibility behavior for capability-driven mobs, not the semantic rank
     * model of the new mob architecture.</p>
     */
    private MobTier determineTier(LivingEntity mob) {
        // Cascading probability roll: MVP > BOSS > MINI_BOSS > ELITE > NORMAL
        double roll = rng.nextDouble();
        double chanceScale = MobStatsConfigAccess.getNaturalTierChanceScale();
        double mvpChance = MobStatsConfigAccess.getMvpChance() * chanceScale;
        double bossChance = MobStatsConfigAccess.getBossChance() * chanceScale;
        double miniBossChance = MobStatsConfigAccess.getMiniBossChance() * chanceScale;
        double eliteChance = MobStatsConfigAccess.getEliteChance() * chanceScale;

        if (roll < mvpChance)
            return MobTier.MVP;
        roll -= mvpChance;
        if (roll < bossChance)
            return MobTier.BOSS;
        roll -= bossChance;
        if (roll < miniBossChance)
            return MobTier.MINI_BOSS;
        roll -= miniBossChance;
        if (roll < eliteChance)
            return MobTier.ELITE;

        return MobTier.NORMAL;
    }

    /**
     * Normalizes a forced tier for natural (non-scripted) spawns.
     * Previously this collapsed all non-NORMAL tiers to ELITE — now it passes
     * the tier as-is so MINI_BOSS, BOSS, and MVP function correctly.
     */
    private MobTier normalizeNaturalTier(MobTier tier) {
        return tier; // All tiers are now valid for natural spawns
    }



    private void applyAttributes(LivingEntity mob, MobStats stats) {
        com.etema.ragnarmmo.system.mobstats.util.MobAttributeHelper.applyAttributes(mob, stats);
    }

    private void normalizeExistingNaturalCreeperBoss(LivingEntity mob, MobStats stats) {
        if (!(mob instanceof Creeper) || !stats.getTier().shouldPersistWorldState()) {
            return;
        }

        BossSpawnMetadata.SpawnInfo spawnInfo = BossSpawnMetadata.read(mob, stats.getTier());
        if (spawnInfo.source() != BossSpawnSource.NATURAL) {
            return;
        }

        MobTier previousTier = stats.getTier();
        stats.setTier(MobTier.ELITE);
        applyAttributes(mob, stats);

        if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getServer() != null) {
            ActiveBossesSavedData.get(serverLevel.getServer()).removeBoss(mob.getUUID());
        }

        SyncMobStatsPacket.fromEntity(mob)
                .ifPresent(packet -> Network.sendTrackingEntityAndSelf(mob, packet));
        RagnarDebugLog.bossWorld(
                "DOWNGRADE natural creeper boss entity={} oldTier={} newTier={}",
                RagnarDebugLog.entityLabel(mob),
                previousTier,
                stats.getTier());
    }
}
