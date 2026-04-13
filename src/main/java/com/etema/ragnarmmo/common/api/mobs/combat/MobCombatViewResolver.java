package com.etema.ragnarmmo.common.api.mobs.combat;

import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority;
import com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthorityResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Resolves the combat view for a given entity using the centralized authority model.
 */
public final class MobCombatViewResolver {

    private MobCombatViewResolver() {
    }

    public static Optional<MobCombatRuntimeView> resolve(LivingEntity entity) {
        MobRuntimeAuthority authority = MobRuntimeAuthorityResolver.classify(entity);

        // Prefer the new profile
        Optional<ComputedMobProfile> profile = ManualMobProfileRuntimeStore.get(entity);
        if (profile.isPresent()) {
            return Optional.of(MobCombatRuntimeView.strict(profile.get()));
        }

        // Handle STRICT category with no profile (this is an error caught by telemetry)
        if (authority == MobRuntimeAuthority.STRICT_NEW_AUTHORITY) {
            return Optional.empty();
        }

        // Fallback to legacy
        return MobStatsProvider.get(entity)
                .filter(MobStats::isInitialized)
                .map(MobCombatRuntimeView::fromLegacy);
    }
}
