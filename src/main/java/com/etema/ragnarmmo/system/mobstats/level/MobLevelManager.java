package com.etema.ragnarmmo.system.mobstats.level;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.mobs.MobScalingMode;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
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
 *
 * <p>The automatic selector still configures only the three non-manual methods. The datapack
 * {@code MANUAL} path bypasses legacy level computation and is surfaced here only as an effective
 * method boundary.</p>
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
        };

        level = Math.max(minByRules, level);
        int maxCap = MobConfig.MAX_LEVEL.get();
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
                || (mobId != null && ManualMobProfileResolver.resolve(mobId).profile() != null);
    }

    private static DifficultyMethodResolution resolveConfiguredAutomaticMethod() {
        return switch (MobConfig.LEVEL_SCALING_MODE.get()) {
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

        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        int variance = Math.max(0, MobConfig.PLAYER_LEVEL_VARIANCE.get());
        int minLevel = Math.max(dimConfig.MIN_FLOOR.get(), anchorLevel - variance);
        int maxLevel = Math.max(minLevel, anchorLevel + variance);
        return pickLevelFromRange(dimConfig, minLevel, maxLevel, tier);
    }

    private Integer resolveNearbyPlayerLevel(ServerLevel serverLevel, LivingEntity mob) {
        int radius = MobConfig.PLAYER_LEVEL_RADIUS.get();
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

        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        double distance = getDistanceFromSpawn(serverLevel, mob.blockPosition());
        List<LevelBand> bands = parseDistanceBands(dimConfig.DISTANCE_BANDS.get());

        Optional<LevelBand> exactBand = findMatchingBand(bands, distance);
        if (exactBand.isPresent()) {
            return pickLevelFromRange(dimConfig, exactBand.get().minLevel(), exactBand.get().maxLevel(), tier);
        }

        Optional<LevelBand> nearestBand = findClosestBand(bands, distance);
        if (nearestBand.isPresent()) {
            warnDistanceNearestBandFallbackOnce();
            return pickLevelFromRange(dimConfig, nearestBand.get().minLevel(), nearestBand.get().maxLevel(), tier);
        }

        warnDistanceFloorFallbackOnce();
        return computeDimensionFloorLevel(serverLevel, tier);
    }

    private int computeBiomeDistanceLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        double distance = getDistanceFromSpawn(serverLevel, mob.blockPosition());
        String biomeId = serverLevel.getBiome(mob.blockPosition())
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("minecraft:plains");

        Optional<LevelBand> biomeBand = findBiomeDistanceBand(dimConfig.BIOME_DISTANCE_BANDS.get(), biomeId, distance);
        if (biomeBand.isPresent()) {
            return pickLevelFromRange(dimConfig, biomeBand.get().minLevel(), biomeBand.get().maxLevel(), tier);
        }

        List<LevelBand> genericBands = parseDistanceBands(dimConfig.DISTANCE_BANDS.get());
        Optional<LevelBand> distanceBand = findMatchingBand(genericBands, distance);
        if (distanceBand.isPresent()) {
            warnBiomeDistanceGenericBandFallbackOnce();
            return pickLevelFromRange(dimConfig, distanceBand.get().minLevel(), distanceBand.get().maxLevel(), tier);
        }

        Optional<LevelBand> nearestGenericBand = findClosestBand(genericBands, distance);
        if (nearestGenericBand.isPresent()) {
            warnBiomeDistanceNearestGenericBandFallbackOnce();
            return pickLevelFromRange(dimConfig,
                    nearestGenericBand.get().minLevel(),
                    nearestGenericBand.get().maxLevel(),
                    tier);
        }

        warnBiomeDistanceFloorFallbackOnce();
        return computeDimensionFloorLevel(serverLevel, tier);
    }

    private int computeDimensionFloorLevel(ServerLevel serverLevel, MobTier tier) {
        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        int floor = dimConfig.MIN_FLOOR.get();
        return pickLevelFromRange(dimConfig, floor, floor, tier);
    }

    private double getDistanceFromSpawn(ServerLevel level, BlockPos pos) {
        BlockPos spawnPos = level.getSharedSpawnPos();
        double dx = pos.getX() - spawnPos.getX();
        double dz = pos.getZ() - spawnPos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int pickLevelFromRange(MobConfig.DimensionConfig dimConfig, int minLevel, int maxLevel, MobTier tier) {
        int floor = dimConfig.MIN_FLOOR.get();
        int cap = dimConfig.MAX_CAP.get();

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

    private List<LevelBand> parseDistanceBands(List<? extends String> entries) {
        List<LevelBand> bands = new ArrayList<>();
        for (String entry : entries) {
            parseDistanceBand(entry).ifPresent(bands::add);
        }
        return bands;
    }

    private Optional<LevelBand> findMatchingBand(List<LevelBand> bands, double distance) {
        for (LevelBand band : bands) {
            if (band.matches(distance)) {
                return Optional.of(band);
            }
        }
        return Optional.empty();
    }

    private Optional<LevelBand> findClosestBand(List<LevelBand> bands, double distance) {
        LevelBand closest = null;
        double closestGap = Double.MAX_VALUE;

        for (LevelBand band : bands) {
            double gap = band.distanceGap(distance);
            if (gap < closestGap) {
                closestGap = gap;
                closest = band;
            }
        }

        return Optional.ofNullable(closest);
    }

    private Optional<LevelBand> findBiomeDistanceBand(List<? extends String> entries, String biomeId, double distance) {
        for (String entry : entries) {
            Optional<BiomeDistanceBand> band = parseBiomeDistanceBand(entry);
            if (band.isPresent() && band.get().matches(biomeId, distance)) {
                return Optional.of(band.get().levelBand());
            }
        }
        return Optional.empty();
    }

    private Optional<LevelBand> parseDistanceBand(String entry) {
        if (entry == null) {
            return Optional.empty();
        }

        int eq = entry.indexOf('=');
        if (eq <= 0 || eq >= entry.length() - 1) {
            return Optional.empty();
        }

        Optional<long[]> distanceRange = parseLongRange(entry.substring(0, eq).trim());
        Optional<int[]> levelRange = parseIntRange(entry.substring(eq + 1).trim());
        if (distanceRange.isEmpty() || levelRange.isEmpty()) {
            return Optional.empty();
        }

        long[] distanceValues = distanceRange.get();
        int[] levelValues = levelRange.get();
        return Optional.of(new LevelBand(distanceValues[0], distanceValues[1], levelValues[0], levelValues[1]));
    }

    private Optional<BiomeDistanceBand> parseBiomeDistanceBand(String entry) {
        if (entry == null) {
            return Optional.empty();
        }

        int pipe = entry.indexOf('|');
        if (pipe <= 0 || pipe >= entry.length() - 1) {
            return Optional.empty();
        }

        String biomeId = entry.substring(0, pipe).trim();
        Optional<LevelBand> band = parseDistanceBand(entry.substring(pipe + 1).trim());
        return band.map(levelBand -> new BiomeDistanceBand(biomeId, levelBand));
    }

    private Optional<long[]> parseLongRange(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String trimmed = text.trim();
        try {
            if (trimmed.endsWith("+")) {
                long min = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                return Optional.of(new long[] { Math.max(0L, min), Long.MAX_VALUE });
            }

            int dash = trimmed.indexOf('-');
            if (dash < 0) {
                long value = Long.parseLong(trimmed);
                return Optional.of(new long[] { Math.max(0L, value), Math.max(0L, value) });
            }

            long min = Long.parseLong(trimmed.substring(0, dash).trim());
            String maxText = trimmed.substring(dash + 1).trim();
            long max = "*".equals(maxText) ? Long.MAX_VALUE : Long.parseLong(maxText);
            if (max < min) {
                max = min;
            }
            return Optional.of(new long[] { Math.max(0L, min), max });
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<int[]> parseIntRange(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String trimmed = text.trim();
        try {
            int dash = trimmed.indexOf('-');
            if (dash < 0) {
                int value = Integer.parseInt(trimmed);
                return Optional.of(new int[] { Math.max(1, value), Math.max(1, value) });
            }

            int min = Integer.parseInt(trimmed.substring(0, dash).trim());
            int max = Integer.parseInt(trimmed.substring(dash + 1).trim());
            if (max < min) {
                max = min;
            }
            return Optional.of(new int[] { Math.max(1, min), Math.max(1, max) });
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private MobConfig.DimensionConfig getDimConfig(ResourceLocation dimId) {
        String idStr = dimId.toString();
        if (idStr.equals("minecraft:the_nether")) {
            return MobConfig.NETHER;
        }
        if (idStr.equals("minecraft:the_end")) {
            return MobConfig.END;
        }
        return MobConfig.OVERWORLD;
    }

    private int computeTierOffset(MobTier tier) {
        return switch (tier) {
            case NORMAL -> rng.nextInt(6) - 2;
            case ELITE, MINI_BOSS, BOSS, MVP -> 3 + rng.nextInt(3);
        };
    }

    private int computeMinByRules(LivingEntity mob) {
        int min = 1;

        String dimId = mob.level().dimension().location().toString();
        min = Math.max(min, ConfigUtils.getMinLevelFor(MobConfig.DIMENSION_MIN_LEVELS.get(), dimId).orElse(1));

        Optional<ResourceLocation> structId = StructureUtils.getStructureAt(mob);
        if (structId.isPresent()) {
            min = Math.max(min,
                    ConfigUtils.getMinLevelFor(MobConfig.STRUCTURE_MIN_LEVELS.get(), structId.get().toString()).orElse(1));
        }

        String entId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        min = Math.max(min, ConfigUtils.getMinLevelFor(MobConfig.BOSS_MIN_LEVELS.get(), entId).orElse(1));
        min = Math.max(min, MobSpawnOverrides.getMinimumLevel(mob).orElse(1));

        return min;
    }

    public DifficultyReport getDifficultyReport(ServerLevel level) {
        List<String> lines = new ArrayList<>();
        MobConfig.LevelScalingMode mode = MobConfig.LEVEL_SCALING_MODE.get();
        MobConfig.DimensionConfig dimConfig = getDimConfig(level.dimension().location());
        DifficultyMethodResolution configuredResolution = resolveConfiguredAutomaticMethod();

        lines.add("Active scaling mode: " + describeScalingMode(mode));
        lines.add("Conceptual methods in the current build: DISTANCE, BIOME_DISTANCE, PLAYER_LEVEL, MANUAL.");
        lines.add("Configured automatic selector values: PLAYER_LEVEL, DISTANCE, BIOME_DISTANCE.");
        lines.add("MANUAL is the datapack/manual runtime path and bypasses the automatic selector when a manual profile exists.");

        switch (mode) {
            case PLAYER_LEVEL -> lines.add(
                    "Hostile mobs anchor to nearby player level, with small random variance. If no valid player is available, PLAYER_LEVEL now resolves to the dimension floor instead of falling through to DISTANCE.");
            case DISTANCE -> lines.add(
                    "Hostile mobs use explicit distance bands from world spawn. If no exact band matches, DISTANCE uses the nearest configured distance band; if none exist, it resolves to the dimension floor.");
            case BIOME_DISTANCE -> lines.add(
                    "Biome-specific distance bands are used first, then generic distance bands, then the nearest generic distance band; if none exist, BIOME_DISTANCE resolves to the dimension floor.");
        }
        lines.add("Configured automatic method today: " + configuredResolution.method() + " via " + configuredResolution.source() + ".");
        lines.add("The removed legacy species TOML path and legacy zone scaling are no longer part of the active difficulty model.");

        lines.add(String.format(
                Locale.ROOT,
                "Dimension floors -> Overworld %d | Nether %d | End %d",
                MobConfig.OVERWORLD.MIN_FLOOR.get(),
                MobConfig.NETHER.MIN_FLOOR.get(),
                MobConfig.END.MIN_FLOOR.get()));
        lines.add(String.format(
                Locale.ROOT,
                "Current dimension floor/cap -> %d / %d",
                dimConfig.MIN_FLOOR.get(),
                dimConfig.MAX_CAP.get()));
        lines.add(String.format(
                Locale.ROOT,
                "Natural elite/boss chance scale -> %.0f%%",
                MobConfig.NATURAL_TIER_CHANCE_SCALE.get() * 100.0D));
        return new DifficultyReport(lines);
    }

    public record DifficultyReport(List<String> lines) {
    }

    private static String describeScalingMode(MobConfig.LevelScalingMode mode) {
        return mode.name();
    }

    private static void warnPlayerLevelFloorFallbackOnce() {
        if (PLAYER_LEVEL_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=PLAYER_LEVEL resolved to the dimension floor because no valid player level anchor was available.");
        }
    }

    private static void warnDistanceNearestBandFallbackOnce() {
        if (DISTANCE_NEAREST_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=DISTANCE did not find an exact distance band match and is using the nearest configured distance band.");
        }
    }

    private static void warnDistanceFloorFallbackOnce() {
        if (DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=DISTANCE has no valid DISTANCE_BANDS configured and resolved to the dimension floor.");
        }
    }

    private static void warnBiomeDistanceGenericBandFallbackOnce() {
        if (BIOME_DISTANCE_GENERIC_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=BIOME_DISTANCE fell back to generic DISTANCE_BANDS because no biome_distance band matched.");
        }
    }

    private static void warnBiomeDistanceNearestGenericBandFallbackOnce() {
        if (BIOME_DISTANCE_NEAREST_GENERIC_BAND_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=BIOME_DISTANCE did not find an exact generic distance band match and is using the nearest generic DISTANCE_BAND.");
        }
    }

    private static void warnBiomeDistanceFloorFallbackOnce() {
        if (BIOME_DISTANCE_FLOOR_FALLBACK_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=BIOME_DISTANCE has no valid generic DISTANCE_BANDS configured and resolved to the dimension floor.");
        }
    }

    private record LevelBand(long minDistance, long maxDistance, int minLevel, int maxLevel) {
        private boolean matches(double distance) {
            return distance >= minDistance && distance <= maxDistance;
        }

        private double distanceGap(double distance) {
            if (matches(distance)) {
                return 0.0D;
            }
            if (distance < minDistance) {
                return minDistance - distance;
            }
            return distance - maxDistance;
        }
    }

    private record BiomeDistanceBand(String biomeId, LevelBand levelBand) {
        private boolean matches(String currentBiomeId, double distance) {
            return ("*".equals(biomeId) || biomeId.equals(currentBiomeId)) && levelBand.matches(distance);
        }
    }
}
