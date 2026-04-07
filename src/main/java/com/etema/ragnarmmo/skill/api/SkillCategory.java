package com.etema.ragnarmmo.skill.api;

/**
 * Categories for skill organization.
 * Extracted from SkillType for data-driven skill system.
 */
public enum SkillCategory {
    CLASS_PASSIVE("Class Tree"),
    LIFE("Life Skill"),
    MISC("Misc"),
    
    // Core Job Categories
    NOVICE("Novice"),
    SWORDSMAN("Swordsman"),
    MAGE("Mage"),
    ARCHER("Archer"),
    THIEF("Thief"),
    MERCHANT("Merchant"),
    ACOLYTE("Acolyte"),
    
    // Advanced Job Categories
    KNIGHT("Knight"),
    WIZARD("Wizard"),
    HUNTER("Hunter"),
    ASSASSIN("Assassin"),
    BLACKSMITH("Blacksmith"),
    PRIEST("Priest");

    private final String displayName;

    SkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static SkillCategory fromId(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return valueOf(id.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
