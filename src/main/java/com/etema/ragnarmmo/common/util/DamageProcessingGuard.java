package com.etema.ragnarmmo.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for preventing double damage processing in RagnarMMO systems.
 *
 * Ensures that when multiple systems (MobStats, PlayerStats) modify the same
 * LivingHurtEvent, they don't stack unintentionally.
 *
 * @author RagnarMMO Team
 * @since 1.0.0
 */
public final class DamageProcessingGuard {

    private static final String TAG_TICK_MOB = "ragnar_damage_tick_mob";
    private static final String TAG_TICK_PLAYER = "ragnar_damage_tick_player";

    private DamageProcessingGuard() {
    }

    /**
     * Checks if damage was processed by the Player system.
     */
    public static boolean isProcessedPlayer(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(TAG_TICK_PLAYER))
            return false;
        return data.getLong(TAG_TICK_PLAYER) == target.level().getGameTime();
    }

    /**
     * Checks if damage was processed by the Mob system.
     */
    public static boolean isProcessedMob(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(TAG_TICK_MOB))
            return false;
        return data.getLong(TAG_TICK_MOB) == target.level().getGameTime();
    }

    /**
     * Marks damage as processed by the Player system.
     */
    public static void markProcessedPlayer(LivingEntity target) {
        target.getPersistentData().putLong(TAG_TICK_PLAYER, target.level().getGameTime());
    }

    /**
     * Marks damage as processed by the Mob system.
     */
    public static void markProcessedMob(LivingEntity target) {
        target.getPersistentData().putLong(TAG_TICK_MOB, target.level().getGameTime());
    }

    /**
     * Clears processing flags. Typically for testing.
     */
    public static void clearProcessed(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(TAG_TICK_PLAYER);
        data.remove(TAG_TICK_MOB);
    }
}
