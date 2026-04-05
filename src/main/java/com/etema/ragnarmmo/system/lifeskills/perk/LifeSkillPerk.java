package com.etema.ragnarmmo.system.lifeskills.perk;

import com.etema.ragnarmmo.skill.api.SkillType;

/**
 * Represents a Life Skill perk that can be chosen every 10 levels.
 * Each tier offers 2 choices (A or B).
 */
public class LifeSkillPerk {

    private final String id;
    private final SkillType skill;
    private final int tier; // 1 = level 10, 2 = level 20, etc.
    private final String choice; // "A" or "B"
    private final String displayName;
    private final String description;
    private final PerkEffect effect;
    private final double value;

    public LifeSkillPerk(String id, SkillType skill, int tier, String choice,
                         String displayName, String description,
                         PerkEffect effect, double value) {
        this.id = id;
        this.skill = skill;
        this.tier = tier;
        this.choice = choice;
        this.displayName = displayName;
        this.description = description;
        this.effect = effect;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getTier() {
        return tier;
    }

    public String getChoice() {
        return choice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public PerkEffect getEffect() {
        return effect;
    }

    public double getValue() {
        return value;
    }

    /**
     * Get the level requirement for this perk (tier * 10).
     */
    public int getLevelRequirement() {
        return tier * 10;
    }

    /**
     * Types of perk effects.
     */
    public enum PerkEffect {
        // Mining
        MINING_SPEED,           // +X% mining speed
        MINING_FORTUNE,         // +X% chance for extra drops
        MINING_DOUBLE_DROP,     // X% chance to double ore drops

        // Woodcutting
        WOODCUTTING_SPEED,      // +X% woodcutting speed
        WOODCUTTING_EXTRA_LOGS, // +X% chance for extra logs
        WOODCUTTING_SAPLING,    // +X% chance for saplings

        // Excavation
        EXCAVATION_SPEED,       // +X% excavation speed
        EXCAVATION_TREASURE,    // +X% treasure chance

        // Farming
        FARMING_EXTRA_CROPS,    // +X% extra crop drops
        FARMING_GROWTH_SPEED,   // +X% crop growth speed (passive)
        FARMING_REPLANT,        // X% auto-replant chance

        // Fishing
        FISHING_SPEED,          // +X% fishing speed
        FISHING_TREASURE,       // +X% treasure chance
        FISHING_LUCK,           // +X luck stat while fishing

        // Exploration
        EXPLORATION_CHEST_LOOT, // +X% better chest loot
        EXPLORATION_MOB_DROP,   // +X% better mob drops
        EXPLORATION_SPEED,      // +X% movement speed
        EXPLORATION_FALL_RESIST // +X% fall damage reduction
    }
}
