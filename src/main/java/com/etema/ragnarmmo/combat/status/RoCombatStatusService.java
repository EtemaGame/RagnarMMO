package com.etema.ragnarmmo.combat.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * RO-domain combat status values stored on entities.
 *
 * Minecraft effects/attributes may still be used for visuals and AI shell
 * behavior, but combat balance reads these tags through CombatantProfile.
 */
public final class RoCombatStatusService {
    public static final String PROVOKE_UNTIL_TAG = "ragnar_provoke_until";
    public static final String PROVOKE_DEF_REDUCTION_TAG = "ragnar_provoke_def_reduction_percent";
    public static final String PROVOKE_ATK_BONUS_TAG = "ragnar_provoke_atk_bonus_percent";
    public static final String DECREASE_AGI_UNTIL_TAG = "ragnar_decrease_agi_until";
    public static final String DECREASE_AGI_AMOUNT_TAG = "ragnar_decrease_agi_amount";

    private RoCombatStatusService() {
    }

    public static void applyProvoke(Mob mob, int durationTicks, double defReductionPercent, double atkBonusPercent) {
        long until = mob.level().getGameTime() + Math.max(1, durationTicks);
        CompoundTag data = mob.getPersistentData();
        data.putLong(PROVOKE_UNTIL_TAG, until);
        data.putDouble(PROVOKE_DEF_REDUCTION_TAG, Math.max(0.0D, defReductionPercent));
        data.putDouble(PROVOKE_ATK_BONUS_TAG, Math.max(0.0D, atkBonusPercent));
    }

    public static void clearProvoke(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(PROVOKE_UNTIL_TAG);
        data.remove(PROVOKE_DEF_REDUCTION_TAG);
        data.remove(PROVOKE_ATK_BONUS_TAG);
    }

    public static void applyDecreaseAgi(LivingEntity target, int durationTicks, int agiReduction) {
        long until = target.level().getGameTime() + Math.max(1, durationTicks);
        CompoundTag data = target.getPersistentData();
        data.putLong(DECREASE_AGI_UNTIL_TAG, until);
        data.putInt(DECREASE_AGI_AMOUNT_TAG, Math.max(0, agiReduction));
    }

    public static void clearDecreaseAgi(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.remove(DECREASE_AGI_UNTIL_TAG);
        data.remove(DECREASE_AGI_AMOUNT_TAG);
    }

    public static double physicalAttackMultiplier(LivingEntity entity) {
        if (!isActive(entity, PROVOKE_UNTIL_TAG)) {
            return 1.0D;
        }
        return 1.0D + (entity.getPersistentData().getDouble(PROVOKE_ATK_BONUS_TAG) / 100.0D);
    }

    public static double physicalDefenseMultiplier(LivingEntity entity) {
        if (!isActive(entity, PROVOKE_UNTIL_TAG)) {
            return 1.0D;
        }
        double reduction = entity.getPersistentData().getDouble(PROVOKE_DEF_REDUCTION_TAG) / 100.0D;
        return Math.max(0.0D, 1.0D - reduction);
    }

    public static int agiPenalty(LivingEntity entity) {
        if (!isActive(entity, DECREASE_AGI_UNTIL_TAG)) {
            return 0;
        }
        return Math.max(0, entity.getPersistentData().getInt(DECREASE_AGI_AMOUNT_TAG));
    }

    public static void clearExpired(LivingEntity entity) {
        if (isExpired(entity, PROVOKE_UNTIL_TAG) && entity instanceof Mob mob) {
            clearProvoke(mob);
        }
        if (isExpired(entity, DECREASE_AGI_UNTIL_TAG)) {
            clearDecreaseAgi(entity);
        }
    }

    private static boolean isActive(LivingEntity entity, String untilTag) {
        long until = entity.getPersistentData().getLong(untilTag);
        return until > entity.level().getGameTime();
    }

    private static boolean isExpired(LivingEntity entity, String untilTag) {
        long until = entity.getPersistentData().getLong(untilTag);
        return until > 0 && until <= entity.level().getGameTime();
    }
}
