package com.etema.ragnarmmo.common.api.lifeskills;

import java.util.Locale;

/**
 * Life Skill types - completely separate from class/combat skills.
 * Uses point-based progression from activities (mining, fishing, etc.)
 *
 * IDs are stable serialized identifiers for life-skill persistence:
 * - mining, woodcutting, excavation, farming, fishing, exploration
 */
public enum LifeSkillType {
    MINING("Mining", "STR", "mining_skill", "pickaxe"),
    WOODCUTTING("Woodcutting", "STR", "woodcutting_skill", "axe"),
    EXCAVATION("Excavation", "VIT", "excavation_skill", "shovel"),
    FARMING("Farming", "DEX", "farming_skill", "hoe"),
    FISHING("Fishing", "LUK", "fishing_skill", "fishing_rod"),
    EXPLORATION("Exploration", "AGI", "exploration_skill", "compass");

    private final String displayName;
    private final String primaryStat;
    private final String textureName;
    private final String iconHint; // Hint for what tool/activity

    LifeSkillType(String displayName, String primaryStat, String textureName, String iconHint) {
        this.displayName = displayName;
        this.primaryStat = primaryStat;
        this.textureName = textureName;
        this.iconHint = iconHint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrimaryStat() {
        return primaryStat;
    }

    public String getTextureName() {
        return textureName;
    }

    public String getIconHint() {
        return iconHint;
    }

    /**
     * Get the serialization ID.
     * Stable serialized id for persistence and networking.
     * e.g., MINING → "mining"
     */
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Get translation key for display name.
     */
    public String getTranslationKey() {
        return "lifeskill.ragnarmmo." + getId();
    }

    /**
     * Get translation key for description.
     */
    public String getDescriptionKey() {
        return "lifeskill.ragnarmmo." + getId() + ".desc";
    }

    /**
     * Parse from string ID (for JSON configs and NBT).
     * Supports both "mining" and "MINING" formats.
     *
     * @param id The skill ID string
     * @return The LifeSkillType or null if not found
     */
    public static LifeSkillType fromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get all life skill types (for iteration).
     */
    public static LifeSkillType[] all() {
        return values();
    }
}
