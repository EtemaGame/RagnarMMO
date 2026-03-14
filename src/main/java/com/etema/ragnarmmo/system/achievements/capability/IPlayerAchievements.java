package com.etema.ragnarmmo.system.achievements.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Set;

public interface IPlayerAchievements extends INBTSerializable<CompoundTag> {

    /**
     * @return the set of achievement IDs the player has completed.
     */
    Set<String> getUnlockedAchievements();

    /**
     * @return the set of achievement IDs the player has claimed rewards for.
     */
    Set<String> getClaimedRewards();

    /**
     * @return the total number of achievement points the player has earned.
     */
    int getTotalPoints();

    /**
     * @return the player's currently active title, or null if none.
     */
    String getActiveTitle();

    /**
     * Set the player's active title. Must be an unlocked title.
     */
    void setActiveTitle(String title);

    /**
     * Mark an achievement as unlocked / completed.
     */
    void unlockAchievement(String id, int pointsAwarded);

    /**
     * Mark an achievement's reward as claimed.
     */
    void claimReward(String id);

    /**
     * Check if an achievement is unlocked.
     */
    boolean isUnlocked(String id);

    /**
     * Check if an achievement's reward is claimed.
     */
    boolean isClaimed(String id);

    /**
     * Sets whether the capability needs to be synced to the client.
     */
    void markDirty();

    boolean isDirty();

    void clearDirty();

    /**
     * Gets a specific tracker's progress for multi-step achievements.
     * 
     * @param id The tracker ID (e.g. "zombie_kills")
     * @return Current progress count
     */
    int getProgress(String id);

    /**
     * Adds progress to a specific tracker.
     */
    void addProgress(String id, int amount);
}
