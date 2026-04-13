package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ManualMobBackendResolver {

    private ManualMobBackendResolver() {
    }

    public record ManualMobBackendResolution(
            ManualCoverageResult coverage,
            @Nullable ComputedMobProfile profile,
            List<String> notes) {
    }

    public static ManualMobBackendResolution resolve(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return resolve(entityTypeId, entity.level().getServer(), MobStatsConfigAccess.getManualMobBackend());
    }

    public static ManualMobBackendResolution resolve(
            ResourceLocation entityTypeId,
            MinecraftServer server,
            RagnarConfigs.ManualMobBackend backend) {
        if (entityTypeId == null) {
            return uncovered(backend, "entityTypeId is null");
        }
        return switch (backend) {
            case INTERNAL -> resolveInternal(entityTypeId, server);
            case DATAPACK -> resolveDatapack(entityTypeId);
            case HYBRID -> resolveHybrid(entityTypeId, server);
        };
    }

    private static ManualMobBackendResolution resolveInternal(ResourceLocation entityTypeId, MinecraftServer server) {
        Optional<ComputedMobProfile> profile = InternalManualMobProfileResolver.resolve(entityTypeId, server);
        if (profile.isPresent()) {
            return covered(RagnarConfigs.ManualMobBackend.INTERNAL, profile.get(), "internal registry match");
        }
        return uncovered(RagnarConfigs.ManualMobBackend.INTERNAL, "internal registry has no enabled entry");
    }

    private static ManualMobBackendResolution resolveDatapack(ResourceLocation entityTypeId) {
        ManualMobProfileResolutionResult result = ManualMobProfileResolver.resolve(entityTypeId);
        if (result.profile() != null) {
            return covered(RagnarConfigs.ManualMobBackend.DATAPACK, result.profile(), "datapack profile resolved");
        }
        return uncovered(RagnarConfigs.ManualMobBackend.DATAPACK, "datapack profile missing or invalid");
    }

    private static ManualMobBackendResolution resolveHybrid(ResourceLocation entityTypeId, MinecraftServer server) {
        ManualMobBackendResolution internal = resolveInternal(entityTypeId, server);
        if (internal.profile != null) {
            return new ManualMobBackendResolution(
                    new ManualCoverageResult(true, RagnarConfigs.ManualMobBackend.INTERNAL, true, "hybrid: internal priority"),
                    internal.profile,
                    List.of("HYBRID priority INTERNAL > DATAPACK"));
        }
        ManualMobBackendResolution datapack = resolveDatapack(entityTypeId);
        if (datapack.profile != null) {
            return new ManualMobBackendResolution(
                    new ManualCoverageResult(true, RagnarConfigs.ManualMobBackend.DATAPACK, true, "hybrid: datapack fallback"),
                    datapack.profile,
                    List.of("HYBRID priority INTERNAL > DATAPACK"));
        }
        return uncovered(RagnarConfigs.ManualMobBackend.HYBRID, "hybrid uncovered in both INTERNAL and DATAPACK");
    }

    private static ManualMobBackendResolution covered(
            RagnarConfigs.ManualMobBackend backend,
            ComputedMobProfile profile,
            String reason) {
        return new ManualMobBackendResolution(
                new ManualCoverageResult(true, backend, true, reason),
                profile,
                List.of(reason));
    }

    private static ManualMobBackendResolution uncovered(
            RagnarConfigs.ManualMobBackend backend,
            String reason) {
        return new ManualMobBackendResolution(
                new ManualCoverageResult(false, backend, false, reason),
                null,
                List.of(reason));
    }
}
