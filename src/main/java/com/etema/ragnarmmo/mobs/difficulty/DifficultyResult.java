package com.etema.ragnarmmo.mobs.difficulty;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DifficultyResult(
        int level,
        MobRank rank,
        int dimensionFloor,
        int dimensionCap,
        Optional<ResourceLocation> matchedStructure,
        Optional<ResourceLocation> matchedBossRule,
        DifficultyMode mode) {
    public DifficultyResult {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        if (rank == null) {
            throw new IllegalArgumentException("rank must not be null");
        }
        matchedStructure = matchedStructure == null ? Optional.empty() : matchedStructure;
        matchedBossRule = matchedBossRule == null ? Optional.empty() : matchedBossRule;
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
    }
}
