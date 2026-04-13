package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

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
}
