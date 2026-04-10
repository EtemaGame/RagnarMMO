package com.etema.ragnarmmo.system.mobstats.level;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;
import com.etema.ragnarmmo.system.mobstats.util.ConfigUtils;
import com.etema.ragnarmmo.system.mobstats.util.StructureUtils;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Legacy hostile-mob level resolver for the old mob-stats pipeline.
 *
 * <p>The scaling modes handled here, including {@code MANUAL_SPECIES}, belong to the config-driven
 * legacy path. They are not the new datapack/manual mob pipeline and should not be treated as the
 * semantic authority for migrated manual content.</p>
 */
public class MobLevelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("RagnarMobStats/MobLevelManager");
    private static final AtomicBoolean LEGACY_MANUAL_SPECIES_WARNING_LOGGED = new AtomicBoolean(false);

    private final Random rng;

    public MobLevelManager(Random rng) {
        this.rng = rng;
    }

    public int computeLevel(LivingEntity mob, SpeciesConfig.SpeciesSettings settings, MobTier tier) {
        int minByRules = computeMinByRules(mob);
        int level = switch (MobConfig.LEVEL_SCALING_MODE.get()) {
            case PLAYER_LEVEL -> computePlayerAnchoredLevel(mob, tier);
            case MANUAL_SPECIES -> {
                warnLegacyManualSpeciesModeOnce();
                yield computeSpeciesAnchoredLevel(mob, settings, tier);
            }
            case BIOME_DISTANCE -> computeBiomeDistanceLevel(mob, tier);
            case DISTANCE -> computeDistanceBasedLevel(mob, tier);
        };

        level = Math.max(minByRules, level);
        int maxCap = MobConfig.MAX_LEVEL.get();
        if (maxCap > 0) {
            level = Math.min(level, maxCap);
        }

        return Math.max(1, level);
    }

    private int computePlayerAnchoredLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        Integer anchorLevel = resolveNearbyPlayerLevel(serverLevel, mob);
        if (anchorLevel == null) {
            return computeDistanceBasedLevel(mob, tier);
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

    private int computeSpeciesAnchoredLevel(LivingEntity mob, SpeciesConfig.SpeciesSettings settings, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        if (settings == null || settings.baseLevel().isEmpty()) {
            return computeDistanceBasedLevel(mob, tier);
        }

        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        int floor = dimConfig.MIN_FLOOR.get();
        int baseLevel = Math.max(floor, settings.baseLevel().getAsInt());
        int variance = Math.max(0, settings.levelVariance().orElse(0));
        int minLevel = Math.max(floor, baseLevel - variance);
        int maxLevel = Math.max(minLevel, baseLevel + variance);

        int level = pickLevelFromRange(dimConfig, minLevel, maxLevel, tier);
        if (settings.maxLevel().isPresent()) {
            level = Math.min(level, Math.max(floor, settings.maxLevel().getAsInt()));
        }
        return level;
    }

    private int computeDistanceBasedLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        double distance = getDistanceFromSpawn(serverLevel, mob.blockPosition());

        Optional<LevelBand> band = findDistanceBand(dimConfig.DISTANCE_BANDS.get(), distance);
        if (band.isPresent()) {
            return pickLevelFromRange(dimConfig, band.get().minLevel(), band.get().maxLevel(), tier);
        }

        return computeLegacyZoneLevel(mob, tier);
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

        Optional<LevelBand> distanceBand = findDistanceBand(dimConfig.DISTANCE_BANDS.get(), distance);
        if (distanceBand.isPresent()) {
            return pickLevelFromRange(dimConfig, distanceBand.get().minLevel(), distanceBand.get().maxLevel(), tier);
        }

        return computeLegacyZoneLevel(mob, tier);
    }

    private int computeLegacyZoneLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        BlockPos mobPos = mob.blockPosition();
        MobConfig.DimensionConfig dimConfig = getDimConfig(serverLevel.dimension().location());
        double distance = getDistanceFromSpawn(serverLevel, mobPos);

        int ringSize = dimConfig.RING_SIZE.get();
        int rings = Math.min((int) (distance / ringSize), dimConfig.RINGS_CAP.get());
        double distanceFactor = Math.pow(dimConfig.DISTANCE_MULTIPLIER.get(), rings);

        double depthFactor = computeDepthFactor(serverLevel, mobPos, dimConfig.DEPTH_RULES.get());
        double structureFactor = StructureUtils.getNearestStructureFactor(mob, dimConfig.STRUCTURE_PROXIMITY.get());

        Holder<Biome> biomeHolder = serverLevel.getBiome(mobPos);
        int[] baseRange = getTierRange(biomeHolder, dimConfig);

        int minLevel = (int) Math.floor(baseRange[0] * distanceFactor * depthFactor * structureFactor);
        int maxLevel = (int) Math.floor(baseRange[1] * distanceFactor * depthFactor * structureFactor);

        return pickLevelFromRange(dimConfig, minLevel, maxLevel, tier);
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

    private Optional<LevelBand> findDistanceBand(List<? extends String> entries, double distance) {
        for (String entry : entries) {
            Optional<LevelBand> band = parseDistanceBand(entry);
            if (band.isPresent() && band.get().matches(distance)) {
                return band;
            }
        }
        return Optional.empty();
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

    private double computeDepthFactor(ServerLevel level, BlockPos pos, List<? extends String> rules) {
        double maxFactor = 1.0;
        boolean canSeeSky = level.canSeeSky(pos);
        int y = pos.getY();

        for (String rule : rules) {
            try {
                String[] parts = rule.split("=");
                if (parts.length < 2) {
                    continue;
                }

                String key = parts[0].toLowerCase(Locale.ROOT);
                double factor = Double.parseDouble(parts[1]);

                boolean applies = false;
                if (key.equals("sky_visible") && canSeeSky) {
                    applies = true;
                } else if (key.equals("no_sky_visible") && !canSeeSky) {
                    applies = true;
                } else if (key.startsWith("y<")) {
                    int limit = Integer.parseInt(key.substring(2));
                    if (y < limit) {
                        applies = true;
                    }
                } else if (key.startsWith("y>")) {
                    int limit = Integer.parseInt(key.substring(2));
                    if (y > limit) {
                        applies = true;
                    }
                }

                if (applies) {
                    maxFactor = Math.max(maxFactor, factor);
                }
            } catch (Exception ignored) {
            }
        }
        return maxFactor;
    }

    private int[] getTierRange(Holder<Biome> biomeHolder, MobConfig.DimensionConfig dimConfig) {
        String tierName = null;
        String biomeId = biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("minecraft:plains");

        for (String entry : dimConfig.BIOME_TO_TIER.get()) {
            String[] parts = entry.split("=");
            if (parts.length >= 2 && parts[0].equals(biomeId)) {
                tierName = parts[1].toLowerCase(Locale.ROOT);
                break;
            }
        }

        if (tierName == null && MobConfig.ENABLE_BIOME_AUTO_CLASSIFY.get()) {
            tierName = classifyBiome(biomeHolder);
        }

        if (tierName == null) {
            tierName = "medium";
        }

        return resolveTier(tierName, dimConfig);
    }

    private String classifyBiome(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_END)) {
            return "very_hard";
        }
        if (biome.is(BiomeTags.IS_NETHER)) {
            return "hard";
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN)) {
            return "hard";
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return "hard";
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return "hard";
        }
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) {
            return "hard";
        }

        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_SAVANNA)) {
            return "medium";
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
            return "medium";
        }
        if (biome.is(BiomeTags.IS_BEACH)) {
            return "easy";
        }

        return "medium";
    }

    private int[] resolveTier(String tierName, MobConfig.DimensionConfig dimConfig) {
        for (String entry : dimConfig.TIERS.get()) {
            String[] parts = entry.split("=");
            if (parts.length >= 2 && parts[0].equalsIgnoreCase(tierName)) {
                String[] range = parts[1].split("-");
                if (range.length >= 2) {
                    try {
                        return new int[] { Integer.parseInt(range[0]), Integer.parseInt(range[1]) };
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return new int[] { 3, 8 };
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

        lines.add("Active scaling mode: " + describeScalingMode(mode));
        lines.add("Available modes: PLAYER_LEVEL, DISTANCE, MANUAL_SPECIES (legacy config-only), BIOME_DISTANCE.");
        lines.add("Note: datapack MANUAL mobs do not use this legacy scaling-mode selector.");

        switch (mode) {
            case PLAYER_LEVEL -> lines.add("Hostile mobs anchor to nearby player level, with small random variance.");
            case DISTANCE -> lines.add("Hostile mobs use explicit distance bands from world spawn.");
            case MANUAL_SPECIES -> lines.add(
                    "Legacy species entries in config/ragnarmmo/mob_species.toml take priority; unlisted mobs fall back to distance bands. This is not the new datapack MANUAL path.");
            case BIOME_DISTANCE -> lines.add("Biome-specific distance bands are used first, then generic distance bands, then legacy biome/depth rules.");
        }

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
        return mode == MobConfig.LevelScalingMode.MANUAL_SPECIES
                ? "MANUAL_SPECIES (legacy config-only)"
                : mode.name();
    }

    private static void warnLegacyManualSpeciesModeOnce() {
        if (LEGACY_MANUAL_SPECIES_WARNING_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "LEVEL_SCALING_MODE=MANUAL_SPECIES is a legacy config-only path backed by mob_species.toml. It is not the new datapack/manual mob pipeline.");
        }
    }

    private record LevelBand(long minDistance, long maxDistance, int minLevel, int maxLevel) {
        private boolean matches(double distance) {
            return distance >= minDistance && distance <= maxDistance;
        }
    }

    private record BiomeDistanceBand(String biomeId, LevelBand levelBand) {
        private boolean matches(String currentBiomeId, double distance) {
            return ("*".equals(biomeId) || biomeId.equals(currentBiomeId)) && levelBand.matches(distance);
        }
    }
}
