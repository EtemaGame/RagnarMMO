package com.etema.ragnarmmo.system.mobstats.integration;

import com.etema.ragnarmmo.common.api.mobs.LegacyMobTierRankMapper;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerDataOrigin;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Legacy integration helper over the old {@link MobStats} capability.
 *
 * <p>This class remains useful for compatibility paths that still consume legacy mob stats
 * directly. New migration work should prefer the shared mob read surface or the client
 * coexistence projection instead of treating this helper as the primary semantic boundary.</p>
 */
public final class MobInfoIntegration {

    private MobInfoIntegration() {
        // Utility class
    }

    /**
     * Gets the legacy mob level from the old capability path.
     *
     * @param entity the living entity to check (nullable)
     * @return OptionalInt containing the legacy mob level, or empty if not available
     */
    @Nonnull
    public static OptionalInt getMobLevel(@Nullable LivingEntity entity) {
        return getCompatibilityMobInfo(entity)
                .map(CompatibilityMobInfo::level)
                .filter(level -> level > 0)
                .stream()
                .mapToInt(Integer::intValue)
                .findFirst();
    }

    /**
     * Gets legacy mob info from the old capability container.
     *
     * @param entity the living entity to check (nullable)
     * @return Optional containing a legacy {@link MobInfo} snapshot, or empty if not available
     */
    @Nonnull
    public static Optional<MobInfo> getMobInfo(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        var readView = MobConsumerReadViewResolver.resolve(entity).orElse(null);
        if (readView != null && readView.dataOrigin() == MobConsumerDataOrigin.NEW_RUNTIME_PROFILE) {
            return Optional.of(new MobInfo(
                    readView.level(),
                    LegacyMobTierRankMapper.toCompatibilityTier(readView.rank())));
        }

        return MobStatsProvider.get(entity)
                .resolve()
                .map(stats -> new MobInfo(
                        stats.getLevel(),
                        stats.getTier()));
    }

    /**
     * Gets the normalized compatibility rank from the old capability path.
     *
     * <p>This is the preferred legacy fallback for coexistence consumers that only need the
     * normalized encounter category and should not keep propagating raw {@link MobTier}.</p>
     */
    @Nonnull
    public static Optional<MobRank> getLegacyCompatibilityRank(@Nullable LivingEntity entity) {
        return getCompatibilityMobInfo(entity)
                .map(CompatibilityMobInfo::rank);
    }

    /**
     * Gets normalized coexistence-safe level/rank info without exposing raw legacy tiers.
     *
     * <p>This prefers the shared read surface when available and otherwise falls back to the old
     * capability path.</p>
     */
    @Nonnull
    public static Optional<CompatibilityMobInfo> getCompatibilityMobInfo(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        var readView = MobConsumerReadViewResolver.resolve(entity).orElse(null);
        if (readView != null) {
            return Optional.of(new CompatibilityMobInfo(
                    readView.level(),
                    readView.rank()));
        }

        return MobStatsProvider.get(entity)
                .resolve()
                .map(stats -> new CompatibilityMobInfo(
                        stats.getLevel(),
                        LegacyMobTierRankMapper.toMobRank(stats.getTier())));
    }

    /**
     * Checks if an entity has mob stats capability attached.
     *
     * @param entity the entity to check
     * @return true if the entity has mob stats
     */
    public static boolean hasMobStats(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return MobStatsProvider.get(entity).isPresent();
    }

    /**
     * Gets the legacy {@link MobTier} for an entity.
     *
     * @param entity the entity to check
     * @return Optional containing the old tier compatibility value, or empty if not available
     */
    @Deprecated(forRemoval = false)
    @Nonnull
    public static Optional<MobTier> getMobTier(@Nullable LivingEntity entity) {
        return getMobInfo(entity)
                .map(MobInfo::tier)
                .filter(Objects::nonNull);
    }

    /**
     * Legacy mob info snapshot from the old mob-stats capability.
     *
     * @param level the legacy mob level (1+)
     * @param tier the legacy compatibility tier
     */
    public record MobInfo(
            int level,
            @Nullable MobTier tier) {

        /**
         * Returns the legacy compatibility display name for the old tier value.
         */
        @Nonnull
        public String getTierDisplayName() {
            return tier != null ? tier.name().toLowerCase() : "normal";
        }
    }

    /**
     * Normalized compatibility snapshot for call sites that only need level/rank semantics.
     */
    public record CompatibilityMobInfo(
            int level,
            MobRank rank) {
    }
}






