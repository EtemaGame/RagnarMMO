package com.etema.ragnarmmo.system.mobstats.integration;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Integration helper for accessing mob stats information.
 * Used by RagnarBar and other modules to retrieve mob level and class info.
 * Thread-safe and null-safe implementation.
 */
public final class MobInfoIntegration {

    private MobInfoIntegration() {
        // Utility class
    }

    /**
     * Gets the level of a mob entity.
     *
     * @param entity the living entity to check (nullable)
     * @return OptionalInt containing the mob's level, or empty if not available
     */
    @Nonnull
    public static OptionalInt getMobLevel(@Nullable LivingEntity entity) {
        if (entity == null) {
            return OptionalInt.empty();
        }

        Optional<MobStats> stats = MobStatsProvider.get(entity).resolve();
        if (stats.isEmpty()) {
            return OptionalInt.empty();
        }

        int level = stats.get().getLevel();
        return level > 0 ? OptionalInt.of(level) : OptionalInt.empty();
    }

    /**
     * Gets the full mob info for an entity.
     *
     * @param entity the living entity to check (nullable)
     * @return Optional containing MobInfo record, or empty if not available
     */
    @Nonnull
    public static Optional<MobInfo> getMobInfo(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return MobStatsProvider.get(entity)
                .resolve()
                .map(stats -> new MobInfo(
                        stats.getLevel(),
                        stats.getMobClass(),
                        stats.getTier()));
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
     * Gets the mob class for an entity.
     *
     * @param entity the entity to check
     * @return Optional containing the mob class, or empty if not available
     */
    @Nonnull
    public static Optional<MobClass> getMobClass(@Nullable LivingEntity entity) {
        return getMobInfo(entity)
                .map(MobInfo::mobClass)
                .filter(Objects::nonNull);
    }

    /**
     * Gets the mob tier for an entity.
     *
     * @param entity the entity to check
     * @return Optional containing the mob tier, or empty if not available
     */
    @Nonnull
    public static Optional<MobTier> getMobTier(@Nullable LivingEntity entity) {
        return getMobInfo(entity)
                .map(MobInfo::tier)
                .filter(Objects::nonNull);
    }

    /**
     * Record containing mob information for display.
     *
     * @param level    the mob's level (1+)
     * @param mobClass the mob's class (warrior, mage, etc.)
     * @param tier     the mob's tier (normal, elite, boss)
     */
    public record MobInfo(
            int level,
            @Nullable MobClass mobClass,
            @Nullable MobTier tier) {

        /**
         * Returns the display name for the mob class.
         */
        @Nonnull
        public String getClassDisplayName() {
            if (mobClass == null)
                return "";
            String name = mobClass.name().toLowerCase();
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        /**
         * Returns the display name for the tier.
         */
        @Nonnull
        public String getTierDisplayName() {
            return tier != null ? tier.name().toLowerCase() : "normal";
        }
    }
}






