package com.etema.ragnarmmo.system.mobstats.service;

import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority;
import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthorityResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.integration.ComputedMobProfileAttributeApplier;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Specialized service for initializing mob runtime state under the new model.
 */
public final class MobRuntimeInitializationService {

    private MobRuntimeInitializationService() {
    }

    /**
     * Attempts to resolve and attach a manual profile for the given entity.
     * 
     * @return true if a profile was successfully attached.
     */
    public static boolean tryInitialize(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (MobStatsConfigAccess.getLevelScalingMode() != RagnarConfigs.LevelScalingMode.MANUAL) {
            return false;
        }

        MobRuntimeAuthority authority = MobRuntimeAuthorityResolver.classify(entity);
        if (authority == MobRuntimeAuthority.LEGACY_ONLY) {
            return false;
        }

        var resolution = ManualMobBackendResolver.resolve(entity);
        if (resolution.profile() != null) {
            var profile = resolution.profile();
            ManualMobProfileRuntimeStore.attach(entity, profile);

            // Apply the full manual runtime surface from a single authority.
            ComputedMobProfileAttributeApplier.apply(entity, profile);
            
            RagnarDebugLog.mobSpawns("Service: Initialized manual profile for {} [authority={}]", 
                    RagnarDebugLog.entityLabel(entity), authority);
            return true;
        } else {
            RagnarDebugLog.migration("Service: Failed to initialize MANUAL mob {} due to: {}",
                    RagnarDebugLog.entityLabel(entity), resolution.notes());
            return false;
        }
    }
}
