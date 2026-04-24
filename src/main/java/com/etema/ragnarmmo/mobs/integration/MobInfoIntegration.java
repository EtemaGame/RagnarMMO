package com.etema.ragnarmmo.mobs.integration;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public final class MobInfoIntegration {

    private MobInfoIntegration() {
        // Utility class
    }

    @Nonnull
    public static OptionalInt getMobLevel(@Nullable LivingEntity entity) {
        return getCompatibilityMobInfo(entity)
                .map(CompatibilityMobInfo::level)
                .filter(level -> level > 0)
                .stream()
                .mapToInt(Integer::intValue)
                .findFirst();
    }

    @Nonnull
    public static Optional<MobInfo> getMobInfo(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return MobConsumerReadViewResolver.resolve(entity)
                .map(readView -> new MobInfo(
                        readView.level(),
                        toMobTier(readView.rank())));
    }

    @Nonnull
    public static Optional<MobRank> getMobRank(@Nullable LivingEntity entity) {
        return getCompatibilityMobInfo(entity)
                .map(CompatibilityMobInfo::rank);
    }

    @Nonnull
    public static Optional<CompatibilityMobInfo> getCompatibilityMobInfo(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return MobConsumerReadViewResolver.resolve(entity)
                .map(readView -> new CompatibilityMobInfo(
                        readView.level(),
                        readView.rank()));
    }

    public static boolean hasMobProfile(@Nullable LivingEntity entity) {
        return entity != null && MobConsumerReadViewResolver.resolve(entity).isPresent();
    }

    @Deprecated(forRemoval = false)
    @Nonnull
    public static Optional<MobTier> getMobTier(@Nullable LivingEntity entity) {
        return getMobInfo(entity)
                .map(MobInfo::tier)
                .filter(Objects::nonNull);
    }

    public record MobInfo(
            int level,
            @Nullable MobTier tier) {

        @Nonnull
        public String getTierDisplayName() {
            return tier != null ? tier.name().toLowerCase() : "normal";
        }
    }

    public record CompatibilityMobInfo(
            int level,
            MobRank rank) {
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






