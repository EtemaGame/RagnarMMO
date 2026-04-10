package com.etema.ragnarmmo.system.mobstats.world;

import com.etema.ragnarmmo.common.api.mobs.LegacyMobTierRankMapper;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerDataOrigin;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class BossTierResolver {

    private BossTierResolver() {
    }

    public static Optional<MobTier> resolveTier(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        Optional<MobTier> projectedNewTier = MobConsumerReadViewResolver.resolve(entity)
                .filter(readView -> readView.dataOrigin() == MobConsumerDataOrigin.NEW_RUNTIME_PROFILE)
                .map(readView -> LegacyMobTierRankMapper.toCompatibilityTier(readView.rank()));
        if (projectedNewTier.isPresent()) {
            return projectedNewTier;
        }

        return MobStatsProvider.get(entity)
                .resolve()
                .map(stats -> stats.getTier());
    }
}
