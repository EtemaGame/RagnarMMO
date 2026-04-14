package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

public final class ManualMobRegistryService {

    private ManualMobRegistryService() {
    }

    public static Optional<InternalManualMobEntry> find(MinecraftServer server, ResourceLocation entityTypeId) {
        if (server == null || entityTypeId == null) {
            return Optional.empty();
        }
        return ManualMobRegistrySavedData.get(server).get(entityTypeId.toString());
    }

    public static InternalManualMobEntry createOrGetStub(MinecraftServer server, ResourceLocation entityTypeId, String editorName) {
        ManualMobRegistrySavedData data = ManualMobRegistrySavedData.get(server);
        return data.get(entityTypeId.toString())
                .orElseGet(() -> {
                    InternalManualMobEntry created = InternalManualMobEntry.createDefault(entityTypeId, editorName);
                    data.upsert(created);
                    return created;
                });
    }

    public static void upsert(MinecraftServer server, InternalManualMobEntry entry) {
        if (server == null || entry == null) {
            return;
        }
        ManualMobRegistrySavedData.get(server).upsert(entry);
    }

    public static boolean remove(MinecraftServer server, ResourceLocation entityTypeId) {
        if (server == null || entityTypeId == null) {
            return false;
        }
        return ManualMobRegistrySavedData.get(server).remove(entityTypeId.toString());
    }

    public static Collection<InternalManualMobEntry> list(MinecraftServer server) {
        if (server == null) {
            return java.util.List.of();
        }
        return java.util.List.copyOf(ManualMobRegistrySavedData.get(server).values());
    }

    public static ManualMobDetail buildDetail(MinecraftServer server, ResourceLocation entityTypeId, @Nullable net.minecraft.server.level.ServerPlayer player) {
        var internalEntry = find(server, entityTypeId).orElse(null);
        var resolution = com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver.resolve(entityTypeId, server, com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess.getManualMobBackend());
        var datapackProfile = com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver.resolve(entityTypeId).profile();

        boolean internalPresent = internalEntry != null;
        boolean internalEnabled = internalEntry != null && internalEntry.enabled();
        boolean datapackCoverage = datapackProfile != null;

        String effectiveBackend = resolution.coverage().covered()
                ? resolution.coverage().backend().name().toLowerCase(java.util.Locale.ROOT)
                : "none";
        boolean manualEffective = resolution.coverage().backend() == com.etema.ragnarmmo.common.config.RagnarConfigs.ManualMobBackend.INTERNAL;

        String scalingMode = com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess.getLevelScalingMode().name().toLowerCase(java.util.Locale.ROOT);
        String backendReason = String.join("; ", resolution.notes());

        boolean isOp = player != null && player.hasPermissions(2);
        boolean editorEnabled = com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess.isManualMobEditorEnabled();

        return new ManualMobDetail(
                internalEntry,
                resolution.profile(),
                internalPresent,
                internalEnabled,
                datapackCoverage,
                effectiveBackend,
                manualEffective,
                scalingMode,
                backendReason,
                editorEnabled && isOp, // canEdit
                editorEnabled && isOp && internalPresent, // canDelete
                editorEnabled && isOp && !internalPresent // canCreate (stub)
        );
    }
}
