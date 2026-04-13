package com.etema.ragnarmmo.system.mobstats.level;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.mobs.MobScalingMode;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.system.mobstats.util.ConfigUtils;
import com.etema.ragnarmmo.system.mobstats.util.StructureUtils;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hostile-mob level resolver for the four effective difficulty methods.
 * Centralized via MobStatsConfigAccess for high-performance snapshot-based scaling.
 */
public class MobLevelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("RagnarMobStats/MobLevelManager");
    private static final AtomicBoolean PLAYER_LEVEL_FLOOR_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean DISTANCE_NEAREST_BAND_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean BIOME_DISTANCE_GENERIC_BAND_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean BIOME_DISTANCE_NEAREST_GENERIC_BAND_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean BIOME_DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED = new AtomicBoolean(false);

    private final Random rng;

    public MobLevelManager(Random rng) {
        this.rng = rng;
    }

    public enum DifficultyMethodSource {
        LEGACY_CONFIG_AUTO,
        DATAPACK
    }

    public record DifficultyMethodResolution(
            MobScalingMode method,
            DifficultyMethodSource source) {
    }

    public record LevelResolution(
            int level,
            DifficultyMethodResolution methodResolution) {
    }

    public int computeLevel(LivingEntity mob, MobTier tier) {
        return computeLevelResolution(mob, tier).level();
    }

    public LevelResolution computeLevelResolution(LivingEntity mob, MobTier tier) {
        int minByRules = computeMinByRules(mob);
        DifficultyMethodResolution methodResolution = resolveConfiguredAutomaticMethod();
        int level = switch (methodResolution.method()) {
            case PLAYER_LEVEL -> computePlayerAnchoredLevel(mob, tier);
            case BIOME_DISTANCE -> computeBiomeDistanceLevel(mob, tier);
            case DISTANCE -> computeDistanceBasedLevel(mob, tier);
            case MANUAL -> throw new IllegalStateException("MANUAL must bypass legacy level computation");
            default -> computeDimensionFloorLevel((ServerLevel) mob.level(), tier);
        };

        level = Math.max(minByRules, level);
        int maxCap = MobStatsConfigAccess.getMaxLevel();
        if (maxCap > 0) {
            level = Math.min(level, maxCap);
        }

        return new LevelResolution(Math.max(1, level), methodResolution);
    }

    public static DifficultyMethodResolution resolveEffectiveMethod(LivingEntity mob) {
        if (isDatapackManualActive(mob)) {
            return new DifficultyMethodResolution(
                    MobScalingMode.MANUAL,
                    DifficultyMethodSource.DATAPACK);
        }
        return resolveConfiguredAutomaticMethod();
    }

    public static boolean isDatapackManualActive(LivingEntity mob) {
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return ManualMobProfileRuntimeStore.get(mob).isPresent()
                || (mobId != null && ManualMobProfileResolver.hasStrictResolvableManualProfile(mobId));
    }

    private static DifficultyMethodResolution resolveConfiguredAutomaticMethod() {
        return switch (MobStatsConfigAccess.getLevelScalingMode()) {
            case PLAYER_LEVEL -> new DifficultyMethodResolution(
                    MobScalingMode.PLAYER_LEVEL,
                    DifficultyMethodSource.LEGACY_CONFIG_AUTO);
            case DISTANCE -> new DifficultyMethodResolution(
                    MobScalingMode.DISTANCE,
                    DifficultyMethodSource.LEGACY_CONFIG_AUTO);
            case BIOME_DISTANCE -> new DifficultyMethodResolution(
                    MobScalingMode.BIOME_DISTANCE,
                    DifficultyMethodSource.LEGACY_CONFIG_AUTO);
        };
    }

    private int computePlayerAnchoredLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        Integer anchorLevel = resolveNearbyPlayerLevel(serverLevel, mob);
        if (anchorLevel == null) {
            warnPlayerLevelFloorFallbackOnce();
            return computeDimensionFloorLevel(serverLevel, tier);
        }

        MobStatsConfigAccess.ParsedDimensionRules dimRules = MobStatsConfigAccess.getDimensionRules(serverLevel.dimension());
        int variance = Math.max(0, MobStatsConfigAccess.getPlayerLevelVariance());
        int minLevel = Math.max(dimRules.minFloor, anchorLevel - variance);
        int maxLevel = Math.max(minLevel, anchorLevel + variance);
        return pickLevelFromRange(dimRules, minLevel, maxLevel, tier);
    }

    private Integer resolveNearbyPlayerLevel(ServerLevel serverLevel, LivingEntity mob) {
        int radius = MobStatsConfigAccess.getPlayerLevelRadius();
        double radiusSqr = radius * (double) radius;
        List<Integer> nearbyLevels = new ArrayList<>();
        Integer closestLevel = null;
        double closestDistanceSqr = Double.MAX_VALUE;

        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            Optional<Integer> levelOpt = RagnarCoreAPI.get(player).map(stats -> Math.max(1, stats.getLevel()));
            if (levelOpt.isEmpty()) {
                continue;
            }

            int level = levelOpt.get();
            double distanceSqr = player.distanceToSqr(mob);
            if (distanceSqr <= radiusSqr) {
                nearbyLevels.add(level);
            }
            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                closestLevel = level;
            }
        }

        if (!nearbyLevels.isEmpty()) {
            int total = nearbyLevels.stream().mapToInt(Integer::intValue).sum();
            return Math.max(1, Math.round((float) total / (float) nearbyLevels.size()));
        }

        return closestLevel;
    }

    private int computeDistanceBasedLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        MobStatsConfigAccess.ParsedDimensionRules dimRules = MobStatsConfigAccess.getDimensionRules(serverLevel.dimension());
        double distance = getDistanceFromSpawn(serverLevel, mob.blockPosition());
        
        Optional<MobStatsConfigAccess.DistanceBand> exactBand = dimRules.distanceBands.stream()
                .filter(b -> distance >= b.minDistance && distance <= b.maxDistance)
                .findFirst();
        
        if (exactBand.isPresent()) {
            return pickLevelFromRange(dimRules, exactBand.get().levelRange.min(), exactBand.get().levelRange.max(), tier);
        }

        Optional<MobStatsConfigAccess.DistanceBand> nearestBand = findClosestBand(dimRules.distanceBands, distance);
        if (nearestBand.isPresent()) {
            warnDistanceNearestBandFallbackOnce();
            return pickLevelFromRange(dimRules, nearestBand.get().levelRange.min(), nearestBand.get().levelRange.max(), tier);
        }

        warnDistanceFloorFallbackOnce();
        return computeDimensionFloorLevel(serverLevel, tier);
    }

    private int computeBiomeDistanceLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        MobStatsConfigAccess.ParsedDimensionRules dimRules = MobStatsConfigAccess.getDimensionRules(serverLevel.dimension());
        double distance = getDistanceFromSpawn(serverLevel, mob.blockPosition());
        ResourceLocation biomeId = serverLevel.getBiome(mob.blockPosition())
                .unwrapKey()
                .map(key -> key.location())
                .orElse(new ResourceLocation("minecraft:plains"));

        Optional<MobStatsConfigAccess.BiomeDistanceBand> biomeBand = dimRules.biomeDistanceBands.stream()
                .filter(b -> b.biome.equals(biomeId) && distance >= b.minDistance && distance <= b.maxDistance)
                .findFirst();

        if (biomeBand.isPresent()) {
            return pickLevelFromRange(dimRules, biomeBand.get().levelRange.min(), biomeBand.get().levelRange.max(), tier);
        }

        Optional<MobStatsConfigAccess.DistanceBand> distanceBand = dimRules.distanceBands.stream()
                .filter(b -> distance >= b.minDistance && distance <= b.maxDistance)
                .findFirst();

        if (distanceBand.isPresent()) {
            warnBiomeDistanceGenericBandFallbackOnce();
            return pickLevelFromRange(dimRules, distanceBand.get().levelRange.min(), distanceBand.get().levelRange.max(), tier);
        }

        Optional<MobStatsConfigAccess.DistanceBand> nearestGenericBand = findClosestBand(dimRules.distanceBands, distance);
        if (nearestGenericBand.isPresent()) {
            warnBiomeDistanceNearestGenericBandFallbackOnce();
            return pickLevelFromRange(dimRules,
                    nearestGenericBand.get().levelRange.min(),
                    nearestGenericBand.get().levelRange.max(),
                    tier);
        }

        warnBiomeDistanceFloorFallbackOnce();
        return computeDimensionFloorLevel(serverLevel, tier);
    }

    private int computeDimensionFloorLevel(ServerLevel serverLevel, MobTier tier) {
        MobStatsConfigAccess.ParsedDimensionRules dimRules = MobStatsConfigAccess.getDimensionRules(serverLevel.dimension());
        int floor = dimRules.minFloor;
        return pickLevelFromRange(dimRules, floor, floor, tier);
    }

    private double getDistanceFromSpawn(ServerLevel level, BlockPos pos) {
        BlockPos spawnPos = level.getSharedSpawnPos();
        double dx = pos.getX() - spawnPos.getX();
        double dz = pos.getZ() - spawnPos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int pickLevelFromRange(MobStatsConfigAccess.ParsedDimensionRules dimRules, int minLevel, int maxLevel, MobTier tier) {
        int floor = dimRules.minFloor;
        int cap = dimRules.maxCap;

        minLevel = Math.max(floor, minLevel);
        maxLevel = Math.max(minLevel, maxLevel);

        if (cap > 0) {
            minLevel = Math.min(minLevel, cap);
            maxLevel = Math.min(maxLevel, cap);
        }
        maxLevel = Math.max(minLevel, maxLevel);

        int baseLevel = minLevel + (maxLevel > minLevel ? rng.nextInt(maxLevel - minLevel + 1) : 0);
        int adjusted = Math.max(floor, baseLevel + computeTierOffset(tier));
        if (cap > 0) {
            adjusted = Math.min(adjusted, cap);
        }
        return Math.max(1, adjusted);
    }

    private Optional<MobStatsConfigAccess.DistanceBand> findClosestBand(List<MobStatsConfigAccess.DistanceBand> bands, double distance) {
        MobStatsConfigAccess.DistanceBand closest = null;
        double closestGap = Double.MAX_VALUE;

        for (MobStatsConfigAccess.DistanceBand band : bands) {
            double gap = computeGap(band, distance);
            if (gap < closestGap) {
                closestGap = gap;
                closest = band;
            }
        }

        return Optional.ofNullable(closest);
    }

    private double computeGap(MobStatsConfigAccess.DistanceBand band, double distance) {
        if (distance >= band.minDistance && distance <= band.maxDistance) return 0;
        if (distance < band.minDistance) return band.minDistance - distance;
        return distance - band.maxDistance;
    }

    private int computeTierOffset(MobTier tier) {
        return switch (tier) {
            case NORMAL -> rng.nextInt(6) - 2;
            case ELITE, MINI_BOSS, BOSS, MVP -> 3 + rng.nextInt(3);
        };
    }

    private int computeMinByRules(LivingEntity mob) {
        int min = 1;

        ResourceLocation dimId = mob.level().dimension().location();
        min = Math.max(min, MobStatsConfigAccess.getDimensionMinLevel(dimId));

        Optional<ResourceLocation> structId = StructureUtils.getStructureAt(mob);
        if (structId.isPresent()) {
            min = Math.max(min, MobStatsConfigAccess.getStructureMinLevel(structId.get()));
        }

        ResourceLocation entId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        min = Math.max(min, MobStatsConfigAccess.getBossMinLevel(entId));
        min = Math.max(min, MobSpawnOverrides.getMinimumLevel(mob).orElse(1));

        return min;
    }

    public DifficultyReport getDifficultyReport(ServerLevel level) {
        List<String> lines = new ArrayList<>();
        RagnarConfigs.LevelScalingMode mode = MobStatsConfigAccess.getLevelScalingMode();
        MobStatsConfigAccess.ParsedDimensionRules dimRules = MobStatsConfigAccess.getDimensionRules(level.dimension());

        lines.add("Active scaling mode: " + mode.name());
        lines.add("Conceptual methods in the current build: DISTANCE, BIOME_DISTANCE, PLAYER_LEVEL, MANUAL.");
        
        lines.add(String.format(Locale.ROOT, "Current dimension floor/cap -> %d / %d", dimRules.minFloor, dimRules.maxCap));
        lines.add(String.format(Locale.ROOT, "Natural tier chance scale -> %.0f%%", MobStatsConfigAccess.getNaturalTierChanceScale() * 100.0D));
        return new DifficultyReport(lines);
    }

    public record DifficultyReport(List<String> lines) {
    }

    private static void warnPlayerLevelFloorFallbackOnce() {
        if (PLAYER_LEVEL_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("PLAYER_LEVEL scaling resolved to floor because no player anchor was found.");
        }
    }

    private static void warnDistanceNearestBandFallbackOnce() {
        if (DISTANCE_NEAREST_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("DISTANCE scaling using nearest configured band.");
        }
    }

    private static void warnDistanceFloorFallbackOnce() {
        if (DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("DISTANCE scaling resolved to floor (no bands found).");
        }
    }

    private static void warnBiomeDistanceGenericBandFallbackOnce() {
        if (BIOME_DISTANCE_GENERIC_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("BIOME_DISTANCE fell back to generic bands.");
        }
    }

    private static void warnBiomeDistanceNearestGenericBandFallbackOnce() {
        if (BIOME_DISTANCE_NEAREST_GENERIC_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("BIOME_DISTANCE fell back to nearest generic band.");
        }
    }

    private static void warnBiomeDistanceFloorFallbackOnce() {
        if (BIOME_DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("BIOME_DISTANCE resolved to floor.");
        }
    }
}
