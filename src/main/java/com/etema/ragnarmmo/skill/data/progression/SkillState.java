package com.etema.ragnarmmo.skill.data.progression;

import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.api.SkillConstants;
import com.etema.ragnarmmo.skill.api.XPGainReason;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Tracks XP and level progress for a single skill.
 * This is the data-driven replacement for SkillProgress.
 * Uses canonical ResourceLocation skill IDs.
 */
public class SkillState {

    private final ResourceLocation skillId;
    private int level;
    private double xp;
    private double xpToNextLevel;
    private long lastProcTime;

    /**
     * Create a new SkillState for a skill.
     *
     * @param skillId The skill's ResourceLocation ID
     */
    public SkillState(ResourceLocation skillId) {
        this.skillId = skillId;
        this.level = 0;
        this.xp = 0;
        this.lastProcTime = 0;
        this.xpToNextLevel = calculateXpToLevel(1);
    }

    /**
     * Create a SkillState from a SkillDefinition.
     *
     * @param definition The skill definition
     */
    public SkillState(ISkillDefinition definition) {
        this(definition.getId());
    }

    /**
     * @return The skill's ResourceLocation ID
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }

    /**
     * @return The current skill level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return The current XP within this level
     */
    public double getXp() {
        return xp;
    }

    /**
     * @return The last time this skill's effect proc'd (gameTime)
     */
    public long getLastProcTime() {
        return lastProcTime;
    }

    /**
     * Set the last proc time.
     *
     * @param lastProcTime The game time when the skill last proc'd
     */
    public void setLastProcTime(long lastProcTime) {
        this.lastProcTime = lastProcTime;
    }

    /**
     * @return XP required to reach the next level
     */
    public double getXpToNextLevel() {
        return xpToNextLevel;
    }

    /**
     * @return Progress percentage toward next level (0.0 to 1.0)
     */
    public double getProgressPercent() {
        int max = getMaxLevel();
        if (level >= max) {
            return 1.0;
        }
        return xpToNextLevel > 0 ? Math.min(1.0, xp / xpToNextLevel) : 0.0;
    }

    /**
     * Get the maximum level for this skill from its definition.
     *
     * @return The maximum level, or default if definition not found
     */
    public int getMaxLevel() {
        return SkillRegistry.get(skillId)
                .map(ISkillDefinition::getMaxLevel)
                .orElse(SkillConstants.DEFAULT_MAX_LEVEL);
    }

    /**
     * Get the XP multiplier for this skill from its definition.
     *
     * @return The XP multiplier, or 1.0 if definition not found
     */
    public double getXpMultiplier() {
        return SkillRegistry.get(skillId)
                .map(ISkillDefinition::getXpMultiplier)
                .orElse(1.0);
    }

    /**
     * Adds XP to this skill and handles level ups.
     *
     * @param amount Base XP amount (will be multiplied by skill modifier)
     * @param reason The reason for gaining XP
     * @return Number of levels gained
     */
    public int addXP(double amount, XPGainReason reason) {
        if (amount <= 0) return 0;

        int maxLevel = getMaxLevel();
        if (level >= maxLevel) {
            return 0;
        }

        // Apply skill-specific multiplier
        this.xp += amount * getXpMultiplier();

        int levelsGained = 0;
        while (this.xp >= this.xpToNextLevel && this.level < maxLevel) {
            this.xp -= this.xpToNextLevel;
            this.level++;
            this.xpToNextLevel = calculateXpToLevel(this.level + 1);
            levelsGained++;
        }

        return levelsGained;
    }

    /**
     * Upgrades the skill by 1 level if below max.
     *
     * @param maxLevel The maximum level to allow
     * @return true if level was upgraded
     */
    public boolean upgradeLevel(int maxLevel) {
        if (level < maxLevel) {
            level++;
            return true;
        }
        return false;
    }

    /**
     * Upgrades the skill by 1 level using its definition's max level.
     *
     * @return true if level was upgraded
     */
    public boolean upgradeLevel() {
        return upgradeLevel(getMaxLevel());
    }

    /**
     * Sets the level directly (for commands/loading).
     *
     * @param level The new level
     */
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, getMaxLevel()));
        this.xp = 0;
        this.xpToNextLevel = calculateXpToLevel(this.level + 1);
    }

    /**
     * Sets XP directly (for loading).
     *
     * @param xp The new XP amount
     */
    public void setXp(double xp) {
        this.xp = Math.max(0, xp);
    }

    /**
     * Resets this skill to level 0 with no XP.
     */
    public void reset() {
        this.level = 0;
        this.xp = 0;
        this.xpToNextLevel = calculateXpToLevel(1);
        this.lastProcTime = 0;
    }

    /**
     * Calculates XP needed to reach a specific level.
     * Formula: BASE_XP * (XP_MULTIPLIER ^ (level - 1))
     *
     * @param targetLevel The target level
     * @return XP required
     */
    public static double calculateXpToLevel(int targetLevel) {
        if (targetLevel <= 0) return 0;
        if (targetLevel == 1) return 100.0; // Fixed cost for 0 -> 1
        return SkillConstants.BASE_XP * Math.pow(SkillConstants.XP_MULTIPLIER, targetLevel - 2);
    }

    /**
     * Gets total XP accumulated across all levels.
     *
     * @return Total XP ever earned for this skill
     */
    public double getTotalXp() {
        double total = xp;
        for (int i = 2; i <= level; i++) {
            total += calculateXpToLevel(i);
        }
        return total;
    }

    @Override
    public String toString() {
        String displayName = SkillRegistry.get(skillId)
                .map(ISkillDefinition::getDisplayName)
                .orElse(skillId.getPath());
        return String.format("%s Lv.%d (%.0f/%.0f XP)", displayName, level, xp, xpToNextLevel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillState that)) return false;
        return skillId.equals(that.skillId);
    }

    @Override
    public int hashCode() {
        return skillId.hashCode();
    }
}
