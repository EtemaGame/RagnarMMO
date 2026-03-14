package com.etema.ragnarmmo.system.skills;

/**
 * Global constants and configuration for the skill system.
 */
public class SkillConstants {
    
    // Global skill configuration
    public static final int DEFAULT_MAX_LEVEL = 100;
    public static final double DEFAULT_BASE_XP = 100.0;
    public static final double DEFAULT_XP_MULTIPLIER = 1.15;
    public static final double XP_TO_LEVEL_1 = 100.0;
    
    // Skill point configuration
    public static final int MAX_SKILL_POINTS = 100;
    public static final int DEFAULT_SKILL_POINT_COST = 1;
    
    // Minimum level for all skills
    public static final int MIN_LEVEL = 0;
    
    // Cart inventory size
    public static final int CART_INVENTORY_SIZE = 54; // 6 rows
    
    // Skill categories
    public static final String CATEGORY_LIFE = "Life";
    public static final String CATEGORY_CLASS = "Class";
    public static final String CATEGORY_MISC = "Misc";
    
    // Novice skill configurations
    public static final int NOVICE_FIRST_AID_MAX_LEVEL = 5;
    public static final int NOVICE_WEAPON_TRAINER_MAX_LEVEL = 10;
    public static final int NOVICE_BASIC_CONDITIONING_MAX_LEVEL = 5;
    public static final int NOVICE_SURVIVAL_INSTINCT_MAX_LEVEL = 5;
    
    // Life skill configurations
    public static final int LIFE_SKILL_MAX_LEVEL = 50;
    
    // Effect application thresholds
    public static final int PASSIVE_SKILL_EFFECT_THRESHOLD = 1; // Effects apply at level 1+
    public static final int ACTIVE_SKILL_EFFECT_THRESHOLD = 1; // Effects apply at level 1+
    
    // XP gain reasons
    public static final String XP_REASON_COMBAT = "Combat";
    public static final String XP_REASON_LIFE_SKILL = "Life Skill";
    public static final String XP_REASON_PASSIVE_UPGRADE = "Passive Upgrade";
    
    // Debug and logging
    public static final boolean DEBUG_SKILL_SYSTEM = false;
    public static final String LOG_PREFIX = "[SkillSystem] ";
}