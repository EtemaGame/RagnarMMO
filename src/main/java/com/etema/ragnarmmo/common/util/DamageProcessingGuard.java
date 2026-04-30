package com.etema.ragnarmmo.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for preventing double damage processing in RagnarMMO systems.
 *
 * Ensures that when multiple combat systems modify the same
 * LivingHurtEvent, they don't stack unintentionally.
 *
 * @author RagnarMMO Team
 * @since 1.0.0
 */
public final class DamageProcessingGuard {

    private static final String TAG_TICK_MOB = "ragnar_damage_tick_mob";
    private static final String TAG_TICK_PLAYER = "ragnar_damage_tick_player";
    private static final String TAG_TICK_BASIC_ATTACK = "ragnar_damage_tick_basic_attack";
    private static final String TAG_TICK_SKILL_CONTRACT = "ragnar_damage_tick_skill_contract";
    private static final String TAG_TICK_RANGED_SNAPSHOT = "ragnar_damage_tick_ranged_snapshot";
    private static final String TAG_TICK_MOB_TO_PLAYER = "ragnar_damage_tick_mob_to_player";
    private static final String TAG_TICK_COMPANION_CONTRACT = "ragnar_damage_tick_companion_contract";

    private DamageProcessingGuard() {
    }

    /**
     * Checks if damage was processed by the Player system.
     */
    public static boolean isProcessedPlayer(LivingEntity target) {
        return isCurrentTick(target, TAG_TICK_PLAYER)
                || isCurrentTick(target, TAG_TICK_BASIC_ATTACK)
                || isCurrentTick(target, TAG_TICK_SKILL_CONTRACT)
                || isCurrentTick(target, TAG_TICK_RANGED_SNAPSHOT)
                || isCurrentTick(target, TAG_TICK_COMPANION_CONTRACT);
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

    public static boolean isProcessedByRagnar(LivingEntity target) {
        return isProcessedPlayer(target)
                || isProcessedMob(target)
                || isCurrentTick(target, TAG_TICK_MOB_TO_PLAYER)
                || isCurrentTick(target, TAG_TICK_COMPANION_CONTRACT);
    }

    /**
     * Marks damage as processed by the Player system.
     */
    public static void markProcessedPlayer(LivingEntity target) {
        target.getPersistentData().putLong(TAG_TICK_PLAYER, target.level().getGameTime());
    }

    public static void markBasicAttack(LivingEntity target) {
        markProcessedPlayer(target);
        target.getPersistentData().putLong(TAG_TICK_BASIC_ATTACK, target.level().getGameTime());
    }

    public static void markSkillContractDamage(LivingEntity target) {
        markProcessedPlayer(target);
        target.getPersistentData().putLong(TAG_TICK_SKILL_CONTRACT, target.level().getGameTime());
    }

    public static void markRangedSnapshot(LivingEntity target) {
        markProcessedPlayer(target);
        target.getPersistentData().putLong(TAG_TICK_RANGED_SNAPSHOT, target.level().getGameTime());
    }

    public static void markMobToPlayerContract(LivingEntity target) {
        target.getPersistentData().putLong(TAG_TICK_MOB_TO_PLAYER, target.level().getGameTime());
    }

    public static void markCompanionContractDamage(LivingEntity target) {
        markProcessedPlayer(target);
        target.getPersistentData().putLong(TAG_TICK_COMPANION_CONTRACT, target.level().getGameTime());
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
        data.remove(TAG_TICK_BASIC_ATTACK);
        data.remove(TAG_TICK_SKILL_CONTRACT);
        data.remove(TAG_TICK_RANGED_SNAPSHOT);
        data.remove(TAG_TICK_MOB_TO_PLAYER);
        data.remove(TAG_TICK_COMPANION_CONTRACT);
    }

    private static boolean isCurrentTick(LivingEntity target, String tag) {
        CompoundTag data = target.getPersistentData();
        return data.contains(tag) && data.getLong(tag) == target.level().getGameTime();
    }
}
