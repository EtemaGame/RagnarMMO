package com.etema.ragnarmmo.system.mobstats.world;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

final class BossTierResolver {

    private BossTierResolver() {
    }

    static Optional<MobTier> resolveTier(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return MobStatsProvider.get(entity)
                .resolve()
                .map(stats -> stats.getTier());
    }
}
