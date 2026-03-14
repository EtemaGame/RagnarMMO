package com.etema.ragnarmmo.system.skills.global;

import com.etema.ragnarmmo.common.api.skills.ISkillDefinition;
import com.etema.ragnarmmo.common.api.skills.SkillCategory;
import com.etema.ragnarmmo.system.skills.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Global manager for common skill systems and configurations.
 * Handles functionality that applies across all skill categories.
 */
public class SkillTypeGlobal {

    // Global constants for all skills
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 100;
    public static final double BASE_XP = 100.0;
    public static final double XP_MULTIPLIER = 1.15;

    // XP requirements for level 0 → 1 (different from other levels)
    public static final double XP_TO_LEVEL_1 = 100.0;

    // Skill point costs and limits
    public static final int MAX_SKILL_POINTS = 100;
    public static final int SKILL_POINT_COST_PER_LEVEL = 1;

    /**
     * Calculates XP needed to reach a specific level.
     * Formula: BASE_XP * (XP_MULTIPLIER ^ (level - 1))
     */
    public static double calculateXpToLevel(int targetLevel) {
        if (targetLevel <= 0)
            return 0;
        if (targetLevel == 1)
            return XP_TO_LEVEL_1;
        return BASE_XP * Math.pow(XP_MULTIPLIER, targetLevel - 2);
    }

    /**
     * Gets the maximum level allowed for a skill.
     */
    public static int getMaxLevel(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(ISkillDefinition::getMaxLevel)
                .orElse(MAX_LEVEL);
    }

    /**
     * Determines if a skill is a passive skill that requires skill points.
     */
    public static boolean isPassiveSkill(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(d -> d.getCategory() == SkillCategory.CLASS_PASSIVE)
                .orElse(false);
    }

    /**
     * Determines if a skill is a life skill.
     */
    public static boolean isLifeSkill(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(d -> d.getCategory() == SkillCategory.LIFE)
                .orElse(false);
    }
}