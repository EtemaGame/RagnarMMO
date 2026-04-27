package com.etema.ragnarmmo.skills.data.progression;

import com.etema.ragnarmmo.skills.api.SkillConstants;
import com.etema.ragnarmmo.skills.api.XPGainReason;
import com.etema.ragnarmmo.skills.runtime.SkillConfigManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Tracks XP and level progress for a ResourceLocation-backed skill.
 */
public class SkillProgress {
    private final ResourceLocation skillId;
    private int level;
    private double xp;
    private double xpToNextLevel;
    private long lastProcTime;

    public SkillProgress(ResourceLocation skillId) {
        this.skillId = skillId;
        this.level = 0;
        this.xp = 0;
        this.lastProcTime = 0;
        this.xpToNextLevel = calculateXpToLevel(1);
    }

    /**
     * Wrapper constructor that copies data from a SkillState.
     */
    public SkillProgress(ResourceLocation skillId, SkillState state) {
        this.skillId = skillId;
        this.level = state.getLevel();
        this.xp = state.getXp();
        this.lastProcTime = state.getLastProcTime();
        this.xpToNextLevel = state.getXpToNextLevel();
    }

    public ResourceLocation getSkillId() {
        return skillId;
    }

    public int getLevel() {
        return level;
    }

    public double getXp() {
        return xp;
    }

    public long getLastProcTime() {
        return lastProcTime;
    }

    public void setLastProcTime(long lastProcTime) {
        this.lastProcTime = lastProcTime;
    }

    public double getXpToNextLevel() {
        return xpToNextLevel;
    }

    public double getProgressPercent() {
        int max = getMaxLevel();
        if (level >= max) {
            return 1.0;
        }
        return xpToNextLevel > 0 ? Math.min(1.0, xp / xpToNextLevel) : 0.0;
    }

    /**
     * Adds XP to this skill and handles level ups.
     *
     * @param amount base XP amount
     * @param reason the reason for gaining XP
     * @return number of levels gained
     */
    public int addXP(double amount, XPGainReason reason) {
        if (amount <= 0)
            return 0;

        if (level >= getMaxLevel()) {
            return 0;
        }

        this.xp += amount;

        int levelsGained = 0;
        while (this.xp >= this.xpToNextLevel && this.level < getMaxLevel()) {
            this.xp -= this.xpToNextLevel;
            this.level++;
            this.xpToNextLevel = calculateXpToLevel(this.level + 1);
            levelsGained++;
        }

        return levelsGained;
    }

    /**
     * Upgrades the skill by 1 level if below max.
     */
    public boolean upgradeLevel(int maxLevel) {
        if (level < maxLevel) {
            level++;
            return true;
        }
        return false;
    }

    /**
     * Sets the level directly (for commands/loading).
     */
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, getMaxLevel()));
        this.xp = 0;
        this.xpToNextLevel = calculateXpToLevel(this.level + 1);
    }

    /**
     * Sets XP directly (for loading).
     */
    public void setXp(double xp) {
        this.xp = Math.max(0, xp);
    }

    /**
     * Calculates XP needed to reach a specific level.
     * Formula: BASE_XP * (XP_MULTIPLIER ^ (level - 1))
     */
    public static double calculateXpToLevel(int targetLevel) {
        if (targetLevel <= 0)
            return 0;
        if (targetLevel == 1)
            return 100.0;
        return SkillConstants.BASE_XP * Math.pow(SkillConstants.XP_MULTIPLIER, targetLevel - 2);
    }

    /**
     * Gets total XP accumulated across all levels.
     */
    public double getTotalXp() {
        double total = xp;
        for (int i = 2; i <= level; i++) {
            total += calculateXpToLevel(i);
        }
        return total;
    }

    public int getMaxLevel() {
        return SkillConfigManager.getMaxLevel(skillId);
    }

    @Override
    public String toString() {
        return String.format("%s Lv.%d (%.0f/%.0f XP)",
                skillId != null ? skillId.getPath() : "unknown", level, xp, xpToNextLevel);
    }
}
