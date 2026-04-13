package com.etema.ragnarmmo.system.mobstats.service;

import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority;
import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthorityResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.system.mobstats.util.MobAttributeHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

        MobRuntimeAuthority authority = MobRuntimeAuthorityResolver.classify(entity);
        if (authority == MobRuntimeAuthority.LEGACY_ONLY) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        var resolution = ManualMobProfileResolver.resolve(id);
        
        if (resolution.isSuccess()) {
            var profile = resolution.profile();
            ManualMobProfileRuntimeStore.attach(entity, profile);
            
            // Apply attributes directly from the profile
            MobAttributeHelper.applyAttributes(entity, profile);
            
            RagnarDebugLog.mobSpawns("Service: Initialized manual profile for {} [authority={}]", 
                    RagnarDebugLog.entityLabel(entity), authority);
            return true;
        } else {
            RagnarDebugLog.migration("Service: Failed to initialize STRICT/COMPAT mob {} due to: {}", 
                    RagnarDebugLog.entityLabel(entity), resolution.issues());
            return false;
        }
    }
}
