package com.etema.ragnarmmo.system.skills;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/**
 * Manages aggro (taunt) state for mobs targeted by Provoke and similar skills.
 *
 * Follows the same PersistentData pattern as StunEvents:
 * - "ragnar_aggro_target" = UUID string of the player to force-target
 * - "ragnar_aggro_until"  = game-time tick when the aggro expires
 *
 * The AggroEvents class polls this each AI tick to override mob targeting.
 */
public final class AggroManager {

    public static final String AGGRO_TARGET_TAG = "ragnar_aggro_target";
    public static final String AGGRO_UNTIL_TAG  = "ragnar_aggro_until";

    private AggroManager() {}

    /**
     * Marks a mob to focus aggro on a specific player for the given duration.
     *
     * @param mob          The mob being taunted.
     * @param tauntedBy    The player who cast Provoke.
     * @param durationTicks How many ticks the aggro lasts.
     */
    public static void applyAggro(Mob mob, LivingEntity tauntedBy, int durationTicks) {
        CompoundTag data = mob.getPersistentData();
        data.putString(AGGRO_TARGET_TAG, tauntedBy.getUUID().toString());
        data.putLong(AGGRO_UNTIL_TAG, mob.level().getGameTime() + durationTicks);
    }

    /**
     * Returns the UUID of the current aggro target, or null if expired / not set.
     *
     * Also cleans up expired aggro automatically.
     */
    public static UUID getAggroTarget(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(AGGRO_TARGET_TAG)) return null;

        long until = data.getLong(AGGRO_UNTIL_TAG);
        if (mob.level().getGameTime() >= until) {
            clearAggro(mob);
            return null;
        }

        try {
            return UUID.fromString(data.getString(AGGRO_TARGET_TAG));
        } catch (IllegalArgumentException e) {
            clearAggro(mob);
            return null;
        }
    }

    /**
     * Removes the aggro mark from a mob (e.g., when it dies or aggro expires).
     */
    public static void clearAggro(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(AGGRO_TARGET_TAG);
        data.remove(AGGRO_UNTIL_TAG);
    }

    /**
     * Returns true if the mob currently has active aggro set.
     */
    public static boolean hasAggro(Mob mob) {
        return getAggroTarget(mob) != null;
    }
}
