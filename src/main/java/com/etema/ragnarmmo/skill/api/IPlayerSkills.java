package com.etema.ragnarmmo.skill.api;

import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for accessing player skills data.
 * Used by other modules (like RagnarBar) to avoid direct dependency on
 * RagnarSkills implementation.
 */
public interface IPlayerSkills {

    /**
     * Gets the total level of the player.
     *
     * @return Sum of all skill levels
     */
    int getTotalLevel();

    /**
     * Resets all skills (levels/xp) through the safe API.
     */
    void resetAll(ChangeReason reason);

    /**
     * Gets the player associated with these skills.
     */
    Player getPlayer();

    /**
     * Gets the current level of a skill by ResourceLocation.
     *
     * @param skillId The skill ID (e.g., "ragnarmmo:bash")
     * @return The level (0 if skill not found)
     */
    int getSkillLevel(ResourceLocation skillId);

    /**
     * Gets the current XP of a skill by ResourceLocation.
     *
     * @param skillId The skill ID
     * @return The current XP amount (0 if skill not found)
     */
    double getSkillXp(ResourceLocation skillId);

    /**
     * Adds XP to a skill by ResourceLocation.
     *
     * @param skillId The skill ID
     * @param amount  The amount of XP to add
     * @param reason  The reason for the XP gain
     * @return number of levels gained
     */
    int addXP(ResourceLocation skillId, double amount, XPGainReason reason);

    /**
     * Adds XP to a skill through the safe API by ResourceLocation.
     *
     * @param skillId The skill ID
     * @param amount  The amount of XP to add
     * @param reason  The reason for the change
     * @return number of levels gained
     */
    int addSkillXP(ResourceLocation skillId, double amount, ChangeReason reason);

    /**
     * Sets a skill level through the safe API by ResourceLocation.
     *
     * @param skillId The skill ID
     * @param level   The new level
     * @param reason  The reason for the change
     * @return the final (clamped) level
     */
    int setSkillLevel(ResourceLocation skillId, int level, ChangeReason reason);

    /**
     * Checks if the player has a specific skill at any level.
     *
     * @param skillId The skill ID
     * @return true if level > 0
     */
    default boolean hasSkill(ResourceLocation skillId) {
        return getSkillLevel(skillId) > 0;
    }

    /**
     * Checks if the player meets the level requirement for a skill.
     *
     * @param skillId       The skill ID
     * @param requiredLevel The required level
     * @return true if current level >= required level
     */
    default boolean meetsSkillRequirement(ResourceLocation skillId, int requiredLevel) {
        return getSkillLevel(skillId) >= requiredLevel;
    }
}
