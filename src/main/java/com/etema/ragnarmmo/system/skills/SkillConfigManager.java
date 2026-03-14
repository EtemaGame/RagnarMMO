package com.etema.ragnarmmo.system.skills;

import com.etema.ragnarmmo.system.skills.global.SkillTypeGlobal;
import com.etema.ragnarmmo.system.skills.data.SkillRegistry;
import com.etema.ragnarmmo.common.api.skills.ISkillDefinition;
import com.etema.ragnarmmo.common.api.skills.SkillCategory;
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
                .orElse(SkillTypeGlobal.MAX_LEVEL);
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
                .orElse(SkillTypeGlobal.SKILL_POINT_COST_PER_LEVEL);
    }
}