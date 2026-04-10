package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;
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
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player)
            return;

        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (ManualMobProfileRuntimeStore.get(mob).isPresent()
                || (mobId != null && ManualMobProfileResolver.resolve(mobId).profile() != null)) {
            return;
        }

        MobStatsProvider.get(mob).ifPresent(stats -> {
            if (stats.isInitialized()) {
                normalizeExistingNaturalCreeperBoss(mob, stats);
                return;
            }

            MobCategory cat = mob.getType().getCategory();
            // Removed exclusion for CREATURE, AMBIENT, WATER_CREATURE to handle them as Lv
            // 1

            if (mobId != null && MobConfig.MOB_EXCLUDE_LIST.get().contains(mobId.toString()))
                return;

            SpeciesConfig.SpeciesSettings species = mobId != null ? SpeciesConfig.get(mobId)
                    : SpeciesConfig.SpeciesSettings.EMPTY;

            // Passive check also includes WATER_AMBIENT (e.g. Salmons, Cod)
            boolean isPassive = cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT
                    || cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT;
            generateStats(mob, stats, species, isPassive);
            stats.setInitialized(true);

            applyAttributes(mob, stats);

            if (stats.getTier().shouldPersistWorldState()) {
                if (BossSpawnMetadata.getSpawnKey(mob).isEmpty()) {
                    BossSpawnMetadata.markNatural(mob);
                }
                if (mob instanceof Mob persistentMob) {
                    persistentMob.setPersistenceRequired();
                }
            }

            if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && stats.getTier().shouldPersistWorldState()
                    && serverLevel.getServer() != null) {
                ActiveBossesSavedData.get(serverLevel.getServer()).registerBoss(serverLevel, mob, stats.getTier());
            }

            RagnarDebugLog.mobSpawns(
                    "SPAWN mob={} pos={} passive={} tier={} level={} class={} stats={} mult[h={},d={},def={},spd={}]",
                    RagnarDebugLog.entityLabel(mob),
                    RagnarDebugLog.blockPos(mob.blockPosition()),
                    isPassive,
                    stats.getTier(),
                    stats.getLevel(),
                    stats.getMobClass(),
                    stats.describeStats(),
                    RagnarDebugLog.formatDouble(stats.getHealthMultiplier()),
                    RagnarDebugLog.formatDouble(stats.getDamageMultiplier()),
                    RagnarDebugLog.formatDouble(stats.getDefenseMultiplier()),
                    RagnarDebugLog.formatDouble(stats.getSpeedMultiplier()));

            // Sincroniza con clientes que ven a este mob (y el propio invocador)
            SyncMobStatsPacket.fromEntity(mob)
                    .ifPresent(packet -> Network.sendTrackingEntityAndSelf(mob, packet));
            
            // Add AI goals based on assigned class
            addMobClassAI(mob, stats.getMobClass());
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

    private void generateStats(LivingEntity mob, MobStats stats, SpeciesConfig.SpeciesSettings species,
            boolean isPassive) {
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
                .orElseGet(() -> determineTier(mob, species));
        stats.setTier(tier);

        int level = levelMgr.computeLevel(mob, species, tier);
        stats.setLevel(level);

        MobClass internalClass = assignInternalClass(mob);
        stats.setMobClass(internalClass);

        int ppl = species.pointsPerLevel().orElse(MobConfig.pointsPerLevel(tier));
        int base = MobConfig.basePoints(tier);
        int total = Math.max(0, base + level * ppl);

        // Map<StatKeys, Integer> weights = calculateInherentWeights(mob); // Old logic
        Map<StatKeys, Integer> weights = new java.util.EnumMap<>(internalClass.getWeights());
        
        // Allow Species TOML to override inherent weights if provided
        if (!species.statWeights().isEmpty() && !species.randomDistribution()) {
            distributor.distribute(stats, total, species);
        } else {
            // Use class-based weights
            distributeWithCalculatedWeights(stats, total, weights);
        }

        double hm = MobConfig.healthMultiplier(tier) * species.healthMultiplier().orElse(1.0) * internalClass.getHpMult();
        double dm = MobConfig.damageMultiplier(tier) * species.damageMultiplier().orElse(1.0) * internalClass.getDmgMult();
        double df = MobConfig.defenseMultiplier(tier) * species.defenseMultiplier().orElse(1.0) * internalClass.getDefMult();
        double sp = species.speedMultiplier().orElse(1.0) * internalClass.getSpdMult();

        // PARTY SCALING
        if (tier != MobTier.NORMAL) {
            double radius = MobConfig.PARTY_SCALING_RADIUS.get();
            long playerCount = mob.level().players().stream()
                    .filter(p -> p.distanceToSqr(mob) < radius * radius)
                    .count();

            if (playerCount > 1) {
                double extraPlayers = playerCount - 1;
                hm *= (1.0 + extraPlayers * MobConfig.PARTY_HP_MULTIPLIER.get());
                dm *= (1.0 + extraPlayers * MobConfig.PARTY_ATK_MULTIPLIER.get());
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
        distributor.distributeByWeights(stats, totalPoints, weights);
    }

    /**
     * Legacy {@link MobTier} selection for the old spawn pipeline.
     *
     * <p>This remains compatibility behavior for capability-driven mobs, not the semantic rank
     * model of the new mob architecture.</p>
     */
    private MobTier determineTier(LivingEntity mob, SpeciesConfig.SpeciesSettings species) {
        if (species != null && species.forcedTier().isPresent())
            return species.forcedTier().get();

        MobTier weightedTier = determineWeightedTier(species);
        if (weightedTier != null) {
            return weightedTier;
        }

        // Cascading probability roll: MVP > BOSS > MINI_BOSS > ELITE > NORMAL
        double roll = rng.nextDouble();
        double chanceScale = MobConfig.NATURAL_TIER_CHANCE_SCALE.get();
        double mvpChance = MobConfig.MVP_CHANCE.get() * chanceScale;
        double bossChance = MobConfig.BOSS_CHANCE.get() * chanceScale;
        double miniBossChance = MobConfig.MINI_BOSS_CHANCE.get() * chanceScale;
        double eliteChance = MobConfig.ELITE_CHANCE.get() * chanceScale;

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



    /**
     * Legacy weighted {@link MobTier} chooser used only by {@link SpeciesConfig} /
     * {@code MANUAL_SPECIES}.
     */
    private MobTier determineWeightedTier(SpeciesConfig.SpeciesSettings species) {
        if (species == null || species.tierWeights().isEmpty()) {
            return null;
        }

        double totalWeight = species.tierWeights().values().stream()
                .filter(weight -> weight != null && weight > 0.0D)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (totalWeight <= 0.0D) {
            return null;
        }

        double roll = rng.nextDouble() * totalWeight;
        for (Map.Entry<MobTier, Double> entry : species.tierWeights().entrySet()) {
            double weight = entry.getValue() == null ? 0.0D : entry.getValue();
            if (weight <= 0.0D) {
                continue;
            }
            roll -= weight;
            if (roll <= 0.0D) {
                return entry.getKey();
            }
        }

        return MobTier.NORMAL;
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
