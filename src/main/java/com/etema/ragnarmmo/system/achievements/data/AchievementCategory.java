package com.etema.ragnarmmo.system.achievements.data;

/**
 * Categories matching the tabs in the Ragnarok Online Achievement System UI.
 */
public enum AchievementCategory {
    BASIC,
    BATTLE,
    ADVENTURE,
    QUEST,
    MEMORIAL_DUNGEON,
    GREAT;

    public String getTranslationKey() {
        return "category.ragnarmmo." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static AchievementCategory fromString(String name) {
        try {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BASIC;
        }
    }
}
