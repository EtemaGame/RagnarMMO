package com.etema.ragnarmmo.skills.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only interface for skill definitions.
 * This is the public API for accessing skill metadata loaded from JSON.
 */
public interface ISkillDefinition {

    // === Identity ===

    /**
     * @return The unique identifier for this skill (e.g., "ragnarmmo:bash")
     */
    ResourceLocation getId();

    /**
     * @return The display name of this skill (e.g., "Bash")
     */
    String getDisplayName();

    /**
     * @return The translated name component for this skill
     */
    Component getTranslatedName();

    /**
     * @return The translation key for this skill's name
     */
    String getTranslationKey();

    /**
     * @return The translation key for this skill's description
     */
    String getDescriptionKey();

    // === Classification ===

    /**
     * @return The category of this skill (CLASS_PASSIVE, LIFE, MISC)
     */
    com.etema.ragnarmmo.skills.api.SkillCategory getCategory();

    /**
     * @return The tier of this skill. Current core scope is NOVICE/FIRST/SECOND/LIFE;
     *         additional tiers may exist as reserved future entries.
     */
    com.etema.ragnarmmo.skills.api.SkillTier getTier();

    /**
     * @return The usage type of this skill (ACTIVE, PASSIVE)
     */
    com.etema.ragnarmmo.skills.api.SkillUsageType getUsageType();

    /**
     * @return true if this skill is active (requires manual activation)
     */
    default boolean isActive() {
        return getUsageType() == com.etema.ragnarmmo.skills.api.SkillUsageType.ACTIVE;
    }

    /**
     * @return The primary stat that scales this skill (STR, AGI, VIT, INT, DEX,
     *         LUK)
     */
    String getScalingStat();

    /**
     * @return XP multiplier for this skill
     */
    double getXpMultiplier();

    // === Costs ===

    /**
     * @return The base resource cost for using this skill (for ACTIVE skills)
     */
    int getBaseCost();

    /**
     * @return Additional cost per skill level
     */
    int getCostPerLevel();

    /**
     * Calculates the total resource cost for this skill at a given level.
     * Formula: baseCost + (level * costPerLevel)
     *
     * @param level The current skill level
     * @return The total resource cost
     */
    default int getResourceCost(int level) {
        if (!isActive())
            return 0;
        return getBaseCost() + (level * getCostPerLevel());
    }

    /**
     * Determines which resource (SP or Mana) this skill consumes.
     */
    default com.etema.ragnarmmo.skills.api.ResourceType getResourceType() {
        return com.etema.ragnarmmo.skills.api.ResourceType.SP;
    }

    // === Timing ===

    /**
     * @return Base cooldown in ticks (20 ticks = 1 second)
     */
    int getCooldownTicks();

    /**
     * @return Base cast delay (global cooldown) in ticks
     */
    int getCastDelayTicks();

    /**
     * @return Base cast time in ticks (0 for instant skills)
     */
    int getCastTimeTicks();

    /**
     * @return Effective cooldown for a specific skill level.
     */
    default int getCooldownTicks(int level) {
        return getCooldownTicks();
    }

    /**
     * @return Effective cast delay for a specific skill level.
     */
    default int getCastDelayTicks(int level) {
        return getCastDelayTicks();
    }

    /**
     * @return Effective cast time for a specific skill level.
     */
    default int getCastTimeTicks(int level) {
        return getCastTimeTicks();
    }

    /**
     * @return true if this skill's cast can be interrupted by damage
     */
    boolean isInterruptible();

    // === Progression ===

    /**
     * @return The maximum level this skill can reach
     */
    int getMaxLevel();

    /**
     * @return Skill point cost to upgrade this skill by one level
     */
    int getUpgradeCost();

    /**
     * @return true if this skill can gain XP from actions
     */
    boolean canGainXp();

    /**
     * @return true if this skill can be upgraded using skill points
     */
    boolean canUpgradeWithPoints();

    // === Requirements ===

    /**
     * @return Map of required skill IDs to minimum levels needed
     */
    Map<ResourceLocation, Integer> getRequirements();

    /**
     * @return Set of job IDs that can learn this skill (empty = all jobs)
     */
    Set<String> getAllowedJobs();

    // === UI ===

    /**
     * @return The texture path for this skill's icon
     */
    ResourceLocation getIcon();

    /**
     * @return The texture name (short form, without path)
     */
    String getTextureName();

    /**
     * @return X position in the skill tree grid
     */
    int getGridX();

    /**
     * @return Y position in the skill tree grid
     */
    int getGridY();

    // === Effect ===

    /**
     * @return The fully qualified class name of the effect implementation, or null
     */
    String getEffectClass();

    // === References & tuning ===

    /**
     * @return Optional reference source name for external balancing data.
     */
    default Optional<String> getReferenceSourceName() {
        return Optional.empty();
    }

    /**
     * @return Optional reference source URL for external balancing data.
     */
    default Optional<String> getReferenceSourceUrl() {
        return Optional.empty();
    }

    /**
     * @return Optional free-form notes describing how the skill was tuned.
     */
    default Optional<String> getReferenceNotes() {
        return Optional.empty();
    }

    /**
     * @return Immutable per-level tuning map loaded from datapacks.
     */
    default Map<Integer, com.etema.ragnarmmo.skills.data.SkillLevelData> getLevelDataMap() {
        return Map.of();
    }

    default Optional<com.etema.ragnarmmo.skills.data.SkillLevelData> getLevelData(int level) {
        return Optional.ofNullable(getLevelDataMap().get(level));
    }

    default int getLevelInt(String key, int level, int defaultValue) {
        return getLevelData(level)
                .flatMap(data -> data.getInt(key))
                .orElse(defaultValue);
    }

    default double getLevelDouble(String key, int level, double defaultValue) {
        return getLevelData(level)
                .flatMap(data -> data.getNumber(key))
                .orElse(defaultValue);
    }

    default Optional<String> getLevelString(String key, int level) {
        return getLevelData(level)
                .flatMap(data -> data.getString(key));
    }

    default boolean getLevelBoolean(String key, int level, boolean defaultValue) {
        return getLevelData(level)
                .flatMap(data -> data.getBoolean(key))
                .orElse(defaultValue);
    }
}
