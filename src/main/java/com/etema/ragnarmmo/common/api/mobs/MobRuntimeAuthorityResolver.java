package com.etema.ragnarmmo.common.api.mobs;

import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

/**
 * Central authority for classifying mobs into migration tiers.
 *
 * <p>This resolver ensures that all systems (client info, tracking packets, combat, capabilities)
 * follow the same source-of-truth rules during the migration period.</p>
 */
public final class MobRuntimeAuthorityResolver {

    private MobRuntimeAuthorityResolver() {
    }

    /**
     * Internal classification logic based on entity type and runtime state.
     */
    public static MobRuntimeAuthority classify(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entity instanceof Player) {
            return MobRuntimeAuthority.LEGACY_ONLY;
        }
        if (MobStatsConfigAccess.getLevelScalingMode() != RagnarConfigs.LevelScalingMode.MANUAL) {
            return MobRuntimeAuthority.LEGACY_ONLY;
        }

        // If the entity already has a manual profile attached, it is treated as STRICT_NEW_AUTHORITY
        // provided it remains healthy.
        if (ManualMobProfileRuntimeStore.get(entity).isPresent()) {
            return MobRuntimeAuthority.STRICT_NEW_AUTHORITY;
        }
        if (ManualMobBackendResolver.resolve(entity).coverage().covered()) {
            return MobRuntimeAuthority.STRICT_NEW_AUTHORITY;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return classify(entityTypeId);
    }

    /**
     * Authority classification based solely on the entity type registry.
     */
    public static MobRuntimeAuthority classify(ResourceLocation entityTypeId) {
        if (MobStatsConfigAccess.getLevelScalingMode() != RagnarConfigs.LevelScalingMode.MANUAL) {
            return MobRuntimeAuthority.LEGACY_ONLY;
        }
        if (entityTypeId == null) {
            return MobRuntimeAuthority.LEGACY_ONLY;
        }

        // Rule for STRICT_NEW_AUTHORITY: A manual definition exists and it is fully resolvable
        // without any validation issues (missing stats, unimplemented derivation, etc).
        if (ManualMobProfileResolver.hasStrictResolvableManualProfile(entityTypeId)) {
            return MobRuntimeAuthority.STRICT_NEW_AUTHORITY;
        }

        // Rule for TEMP_COMPAT: A manual definition exists (is "covered") but it is either
        // incomplete or has unresolved runtime issues.
        if (ManualMobProfileResolver.hasManualDefinition(entityTypeId)) {
            return MobRuntimeAuthority.TEMP_COMPAT;
        }

        return MobRuntimeAuthority.LEGACY_ONLY;
    }
}
