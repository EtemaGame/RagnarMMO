package com.etema.ragnarmmo.system.mobstats.level;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;
import com.etema.ragnarmmo.system.mobstats.util.ConfigUtils;
import com.etema.ragnarmmo.system.mobstats.util.StructureUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public class MobLevelManager {

    private final Random rng;

    public MobLevelManager(Random rng) {
        this.rng = rng;
    }

    public int computeLevel(LivingEntity mob, SpeciesConfig.SpeciesSettings settings, MobTier tier) {
        // 1. Minimum-level rules (dim/structure/boss)
        int minByRules = computeMinByRules(mob);

        // 2. Main Leveling System (Phase 2 Zone Scaling)
        int level = computeZoneBasedLevel(mob, tier);

        // 3. Final clamp
        level = Math.max(minByRules, level);
        int maxCap = MobConfig.MAX_LEVEL.get();
        if (maxCap > 0) {
            level = Math.min(level, maxCap);
        }

        return Math.max(1, level);
    }

    private int computeZoneBasedLevel(LivingEntity mob, MobTier tier) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return 1;
        }

        MobCategory cat = mob.getType().getCategory();
        boolean isPassive = cat == MobCategory.CREATURE
                || cat == MobCategory.AMBIENT
                || cat == MobCategory.WATER_CREATURE;
        if (isPassive) {
            return 1;
        }

        BlockPos mobPos = mob.blockPosition();
        ResourceLocation dimId = serverLevel.dimension().location();
        MobConfig.DimensionConfig dimConfig = getDimConfig(dimId);

        BlockPos spawnPos = serverLevel.getSharedSpawnPos();
        double dx = mobPos.getX() - spawnPos.getX();
        double dz = mobPos.getZ() - spawnPos.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        int ringSize = dimConfig.RING_SIZE.get();
        int rings = Math.min((int) (distance / ringSize), dimConfig.RINGS_CAP.get());
        double distanceFactor = Math.pow(dimConfig.DISTANCE_MULTIPLIER.get(), rings);

        double depthFactor = computeDepthFactor(serverLevel, mobPos, dimConfig.DEPTH_RULES.get());
        double structureFactor = StructureUtils.getNearestStructureFactor(mob, dimConfig.STRUCTURE_PROXIMITY.get());

        Holder<Biome> biomeHolder = serverLevel.getBiome(mobPos);
        int[] baseRange = getTierRange(biomeHolder, dimConfig);

        int minLvl = (int) Math.floor(baseRange[0] * distanceFactor * depthFactor * structureFactor);
        int maxLvl = (int) Math.floor(baseRange[1] * distanceFactor * depthFactor * structureFactor);

        minLvl = Math.max(minLvl, dimConfig.MIN_FLOOR.get());
        maxLvl = Math.max(maxLvl, minLvl);
        int ringCap = dimConfig.MAX_CAP.get();
        if (ringCap > 0)
            maxLvl = Math.min(maxLvl, ringCap);

        int baseLevel = minLvl + (maxLvl > minLvl ? rng.nextInt(maxLvl - minLvl + 1) : 0);
        int tierOffset = computeTierOffset(tier);

        return Math.max(1, baseLevel + tierOffset);
    }

    private MobConfig.DimensionConfig getDimConfig(ResourceLocation dimId) {
        String idStr = dimId.toString();
        if (idStr.equals("minecraft:the_nether"))
            return MobConfig.NETHER;
        if (idStr.equals("minecraft:the_end"))
            return MobConfig.END;
        return MobConfig.OVERWORLD;
    }

    private double computeDepthFactor(ServerLevel level, BlockPos pos, List<? extends String> rules) {
        double maxFactor = 1.0;
        boolean canSeeSky = level.canSeeSky(pos);
        int y = pos.getY();

        for (String rule : rules) {
            try {
                String[] parts = rule.split("=");
                if (parts.length < 2)
                    continue;
                String key = parts[0].toLowerCase(Locale.ROOT);
                double factor = Double.parseDouble(parts[1]);

                boolean applies = false;
                if (key.equals("sky_visible") && canSeeSky)
                    applies = true;
                else if (key.equals("no_sky_visible") && !canSeeSky)
                    applies = true;
                else if (key.startsWith("y<")) {
                    int limit = Integer.parseInt(key.substring(2));
                    if (y < limit)
                        applies = true;
                } else if (key.startsWith("y>")) {
                    int limit = Integer.parseInt(key.substring(2));
                    if (y > limit)
                        applies = true;
                }

                if (applies)
                    maxFactor = Math.max(maxFactor, factor);
            } catch (Exception e) {
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
        if (biome.is(BiomeTags.IS_END))
            return "very_hard";
        if (biome.is(BiomeTags.IS_NETHER))
            return "hard";
        if (biome.is(BiomeTags.IS_MOUNTAIN))
            return "hard";
        if (biome.is(BiomeTags.IS_BADLANDS))
            return "hard";
        if (biome.is(BiomeTags.IS_JUNGLE))
            return "hard";
        if (biome.is(BiomeTags.IS_DEEP_OCEAN))
            return "hard";

        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_SAVANNA))
            return "medium";
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER))
            return "medium";

        if (biome.is(BiomeTags.IS_BEACH))
            return "easy";

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
        return new int[] { 3, 8 }; // Fallback Medium
    }

    private int computeTierOffset(MobTier tier) {
        int roll = rng.nextInt(100);
        if (roll < 80)
            return rng.nextInt(6) - 2; // NORMAL
        else if (roll < 95)
            return 3 + rng.nextInt(3); // ELITE
        else
            return 5 + rng.nextInt(3); // BOSS
    }

    private int computeMinByRules(LivingEntity mob) {
        int min = 1;

        String dimId = mob.level().dimension().location().toString();
        min = Math.max(min, ConfigUtils.getMinLevelFor(
                MobConfig.DIMENSION_MIN_LEVELS.get(), dimId).orElse(1));

        Optional<ResourceLocation> structId = StructureUtils.getStructureAt(mob);
        if (structId.isPresent()) {
            min = Math.max(min, ConfigUtils.getMinLevelFor(
                    MobConfig.STRUCTURE_MIN_LEVELS.get(), structId.get().toString()).orElse(1));
        }

        String entId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        min = Math.max(min, ConfigUtils.getMinLevelFor(
                MobConfig.BOSS_MIN_LEVELS.get(), entId).orElse(1));

        return min;
    }

    public DifficultyReport getDifficultyReport(ServerLevel level) {
        List<String> lines = new ArrayList<>();
        lines.add("Advanced Zone Scaling (Phase 2) is ACTIVE.");
        lines.add("Prioritizes geographical factors (Distance, Biomes, Depth, Structures).");
        lines.add("Dimension-specific configs for Overworld, Nether, and End are used.");

        return new DifficultyReport(lines);
    }

    public record DifficultyReport(List<String> lines) {
    }
}
