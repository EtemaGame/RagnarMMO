package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerDataOrigin;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class BossRankResolver {

    private BossRankResolver() {
    }

    public static Optional<MobRank> resolveRank(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        Optional<MobRank> projectedRank = MobConsumerReadViewResolver.resolve(entity)
                .filter(readView -> readView.dataOrigin() == MobConsumerDataOrigin.NEW_RUNTIME_PROFILE)
                .map(readView -> readView.rank());
        if (projectedRank.isPresent()) {
            return projectedRank;
        }

        return Optional.empty();
    }
}
