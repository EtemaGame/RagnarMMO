package com.etema.ragnarmmo.skills.api;

/**
 * Global constants for the skill system.
 */
public final class SkillConstants {

    // XP thresholds
    public static final double BASE_XP = 100.0;
    public static final double XP_MULTIPLIER = 1.5;
    public static final int BASE_XP_PER_LEVEL = 100;
    public static final double XP_CURVE_EXPONENT = 1.5;

    // Progression Defaults
    public static final int DEFAULT_MAX_LEVEL = 20;
    public static final int SKILL_POINT_COST_PER_LEVEL = 1;

    // Casting
    public static final int MIN_CAST_TIME = 0; // Ticks
    public static final double CAST_TIME_REDUCTION_CAP = 0.5; // 50% max reduction

    // Damage
    public static final double PVP_DAMAGE_MULTIPLIER = 0.5;

    // Syncing
    public static final int SYNC_INTERVAL_TICKS = 20;

    private SkillConstants() {
        // Utility class
    }
}
