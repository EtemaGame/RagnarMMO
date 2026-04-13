package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public final class InternalManualMobProfileResolver {

    private InternalManualMobProfileResolver() {
    }

    public static Optional<ComputedMobProfile> resolve(ResourceLocation entityTypeId, MinecraftServer server) {
        return ManualMobRegistryService.find(server, entityTypeId)
                .filter(entry -> entry.enabled())
                .flatMap(entry -> {
                    try {
                        return Optional.of(entry.toComputedMobProfile());
                    } catch (Exception ignored) {
                        return Optional.empty();
                    }
                });
    }
}
