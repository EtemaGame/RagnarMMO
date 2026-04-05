package com.etema.ragnarmmo.system.mobstats.world;

import com.etema.ragnarmmo.common.api.mobs.MobTier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.OptionalInt;

public final class MobSpawnOverrides {

    private static final String ROOT_KEY = "RagnarMobSpawnOverrides";
    private static final String FORCED_TIER_KEY = "ForcedTier";
    private static final String MINIMUM_LEVEL_KEY = "MinimumLevel";
    private static final String MANUAL_BOSS_KEY = "ManualBoss";

    private MobSpawnOverrides() {
    }

    public static void setForcedTier(LivingEntity entity, MobTier tier) {
        if (entity == null || tier == null) {
            return;
        }

        CompoundTag root = getOrCreateRoot(entity);
        root.putString(FORCED_TIER_KEY, tier.name());
    }

    public static Optional<MobTier> getForcedTier(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        CompoundTag root = getRoot(entity);
        if (root == null || !root.contains(FORCED_TIER_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }

        try {
            return Optional.of(MobTier.valueOf(root.getString(FORCED_TIER_KEY)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Optional<MobTier> consumeForcedTier(LivingEntity entity) {
        Optional<MobTier> tier = getForcedTier(entity);
        clearForcedTier(entity);
        return tier;
    }

    public static void setMinimumLevel(LivingEntity entity, int level) {
        if (entity == null) {
            return;
        }

        CompoundTag root = getOrCreateRoot(entity);
        root.putInt(MINIMUM_LEVEL_KEY, Math.max(1, level));
    }

    public static OptionalInt getMinimumLevel(LivingEntity entity) {
        if (entity == null) {
            return OptionalInt.empty();
        }

        CompoundTag root = getRoot(entity);
        if (root == null || !root.contains(MINIMUM_LEVEL_KEY, Tag.TAG_INT)) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Math.max(1, root.getInt(MINIMUM_LEVEL_KEY)));
    }

    public static void setManualBoss(LivingEntity entity, boolean manualBoss) {
        if (entity == null) {
            return;
        }

        CompoundTag root = getOrCreateRoot(entity);
        root.putBoolean(MANUAL_BOSS_KEY, manualBoss);
    }

    public static boolean isManualBoss(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        CompoundTag root = getRoot(entity);
        return root != null && root.getBoolean(MANUAL_BOSS_KEY);
    }

    public static void clearManualBoss(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        CompoundTag root = getRoot(entity);
        if (root == null) {
            return;
        }

        root.remove(MANUAL_BOSS_KEY);
        root.remove(MINIMUM_LEVEL_KEY);
        cleanupRoot(entity);
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        entity.getPersistentData().remove(ROOT_KEY);
    }

    private static void clearForcedTier(LivingEntity entity) {
        CompoundTag root = getRoot(entity);
        if (root == null) {
            return;
        }

        root.remove(FORCED_TIER_KEY);
        cleanupRoot(entity);
    }

    private static CompoundTag getOrCreateRoot(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            data.put(ROOT_KEY, new CompoundTag());
        }
        return data.getCompound(ROOT_KEY);
    }

    private static CompoundTag getRoot(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return data.getCompound(ROOT_KEY);
    }

    private static void cleanupRoot(LivingEntity entity) {
        CompoundTag root = getRoot(entity);
        if (root == null || !root.isEmpty()) {
            return;
        }
        entity.getPersistentData().remove(ROOT_KEY);
    }
}
