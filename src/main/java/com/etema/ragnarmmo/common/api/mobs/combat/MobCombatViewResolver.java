package com.etema.ragnarmmo.common.api.mobs.combat;

import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Resolves the combat view for a given entity using the centralized authority model.
 */
public final class MobCombatViewResolver {

    private MobCombatViewResolver() {
    }

    public static Optional<MobCombatRuntimeView> resolve(LivingEntity entity) {
        Optional<MobCombatRuntimeView> profileView = MobProfileProvider.get(entity)
                .resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .map(MobCombatRuntimeView::fromProfile);
        if (profileView.isPresent()) {
            return profileView;
        }

        return Optional.empty();
    }
}
