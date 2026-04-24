package com.etema.ragnarmmo.mobs.difficulty;

import java.util.Optional;
import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record DifficultyContext(
        ResourceLocation entityType,
        ResourceLocation dimension,
        BlockPos mobPos,
        BlockPos worldSpawnPos,
        Optional<ResourceLocation> structureId,
        OptionalInt nearestPlayerLevel,
        long worldSeed) {
}
