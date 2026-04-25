package com.etema.ragnarmmo.skills.runtime;

import com.etema.ragnarmmo.skills.api.SkillConstants;
import com.etema.ragnarmmo.skills.api.XPGainReason;
import com.etema.ragnarmmo.skills.data.progression.SkillProgress;
import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.skills.api.SkillCategory;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Central configuration manager for skill systems.
 * Provides unified access to skill properties via SkillRegistry.
 */
public class SkillConfigManager {

    /**
     * Gets the maximum level for a skill.
     */
    public static int getMaxLevel(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(ISkillDefinition::getMaxLevel)
                .orElse(SkillConstants.DEFAULT_MAX_LEVEL);
    }

    /**
     * Determines if a skill can gain XP through usage.
     */
    public static boolean canGainXp(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(def -> def.getCategory() == SkillCategory.LIFE)
                .orElse(false);
    }

    /**
     * Handles skill progression.
     */
    public static boolean handleProgression(SkillProgress progress, double amount) {
        ResourceLocation skillId = progress.getSkillId();
        if (canGainXp(skillId)) {
            if (amount <= 0)
                return false;
            return progress.addXP(amount, XPGainReason.SKILL_USE) > 0;
        }
        return false;
    }

    /**
     * Determines if a skill can be upgraded with skill points.
     */
    public static boolean canUpgradeWithSkillPoints(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(def -> def.getCategory() == SkillCategory.CLASS_PASSIVE)
                .orElse(false);
    }

    /**
     * Determines if a skill can apply effects.
     */
    public static boolean canApplyEffects(SkillProgress progress) {
        return progress.getLevel() >= 1;
    }

    /**
     * Gets the upgrade cost for a skill that uses skill points.
     */
    public static int getUpgradeCost(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(ISkillDefinition::getUpgradeCost)
                .orElse(SkillConstants.SKILL_POINT_COST_PER_LEVEL);
    }
}
