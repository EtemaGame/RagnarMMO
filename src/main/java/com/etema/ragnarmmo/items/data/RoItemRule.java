package com.etema.ragnarmmo.items.data;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.StatKeys;

import java.util.Map;
import java.util.Set;

/**
 * Immutable record representing RO-style item properties.
 * Contains attribute bonuses, requirements, and display information.
 */
public record RoItemRule(
        String displayName, // e.g., "Blade [3]" or null to use vanilla name
        Map<StatKeys, Integer> attributeBonuses, // STR -> +5, DEX -> +3, etc.
        int requiredBaseLevel, // 0 = no level requirement
        Set<JobType> allowedJobs, // empty = all jobs allowed
        int cardSlots, // number of card slots (for future use)
        boolean showTooltip, // true = show RO combat block even if only base-type data exists
        RoCombatProfile combatProfile // optional manual combat compatibility for external weapons
) {
    /**
     * Empty rule used when no rules apply to an item.
     */
    public static final RoItemRule EMPTY = new RoItemRule(
            null, Map.of(), 0, Set.of(), 0, false, RoCombatProfile.EMPTY);

    /**
     * Defensive copy constructor to ensure immutability.
     */
    public RoItemRule {
        attributeBonuses = attributeBonuses != null ? Map.copyOf(attributeBonuses) : Map.of();
        allowedJobs = allowedJobs != null ? Set.copyOf(allowedJobs) : Set.of();
        combatProfile = combatProfile != null ? combatProfile : RoCombatProfile.EMPTY;
    }

    /**
     * @return true if this rule has any requirements (level or class)
     */
    public boolean hasRequirements() {
        return requiredBaseLevel > 0 || !allowedJobs.isEmpty();
    }

    /**
     * @return true if this rule provides attribute bonuses
     */
    public boolean hasAttributeBonuses() {
        return !attributeBonuses.isEmpty();
    }

    public boolean hasCombatProfile() {
        return combatProfile != null && !combatProfile.isEmpty();
    }

    /**
     * @return true if this rule has any meaningful data
     */
    public boolean isEmpty() {
        return this == EMPTY || (!hasRequirements() && !hasAttributeBonuses()
                && !hasCombatProfile() && cardSlots == 0 && !showTooltip);
    }

    /**
     * Get the attribute bonus for a specific stat.
     * 
     * @param stat the stat key
     * @return the bonus value, or 0 if not present
     */
    public int getBonus(StatKeys stat) {
        return attributeBonuses.getOrDefault(stat, 0);
    }

    /**
     * Merge a less specific rule into a more specific one.
     * Numbers use 0 as "unspecified", so non-zero values from {@code override}
     * replace the base. Attribute bonuses are accumulated.
     */
    public static RoItemRule merge(RoItemRule base, RoItemRule override) {
        if (base == null || base.isEmpty()) {
            return override != null ? override : EMPTY;
        }
        if (override == null || override.isEmpty()) {
            return base;
        }

        Map<StatKeys, Integer> mergedBonuses = new java.util.EnumMap<>(StatKeys.class);
        mergedBonuses.putAll(base.attributeBonuses());
        override.attributeBonuses().forEach((key, value) ->
                mergedBonuses.put(key, mergedBonuses.getOrDefault(key, 0) + value));

        String mergedDisplayName = override.displayName() != null ? override.displayName() : base.displayName();
        int mergedRequiredBaseLevel = override.requiredBaseLevel() > 0
                ? override.requiredBaseLevel()
                : base.requiredBaseLevel();
        Set<JobType> mergedAllowedJobs = !override.allowedJobs().isEmpty()
                ? override.allowedJobs()
                : base.allowedJobs();
        int mergedCardSlots = override.cardSlots() > 0 ? override.cardSlots() : base.cardSlots();
        boolean mergedShowTooltip = base.showTooltip() || override.showTooltip();
        RoCombatProfile mergedCombatProfile = RoCombatProfile.merge(base.combatProfile(), override.combatProfile());

        return new RoItemRule(
                mergedDisplayName,
                mergedBonuses,
                mergedRequiredBaseLevel,
                mergedAllowedJobs,
                mergedCardSlots,
                mergedShowTooltip,
                mergedCombatProfile);
    }
}
