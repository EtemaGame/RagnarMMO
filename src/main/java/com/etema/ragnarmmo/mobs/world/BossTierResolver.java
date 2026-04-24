package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerDataOrigin;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;

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
                .map(readView -> toMobTier(readView.rank()));
        if (projectedNewTier.isPresent()) {
            return projectedNewTier;
        }

        return Optional.empty();
    }

    private static MobTier toMobTier(MobRank rank) {
        if (rank == null) {
            return MobTier.NORMAL;
        }
        return switch (rank) {
            case NORMAL -> MobTier.NORMAL;
            case ELITE -> MobTier.ELITE;
            case MINI_BOSS -> MobTier.MINI_BOSS;
            case BOSS -> MobTier.BOSS;
            case MVP -> MobTier.MVP;
        };
    }
}
