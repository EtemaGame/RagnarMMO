package com.etema.ragnarmmo.roitems.data;

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
        int cardSlots // number of card slots (for future use)
) {
    /**
     * Empty rule used when no rules apply to an item.
     */
    public static final RoItemRule EMPTY = new RoItemRule(
            null, Map.of(), 0, Set.of(), 0);

    /**
     * Defensive copy constructor to ensure immutability.
     */
    public RoItemRule {
        attributeBonuses = attributeBonuses != null ? Map.copyOf(attributeBonuses) : Map.of();
        allowedJobs = allowedJobs != null ? Set.copyOf(allowedJobs) : Set.of();
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

    /**
     * @return true if this rule has any meaningful data
     */
    public boolean isEmpty() {
        return this == EMPTY || (!hasRequirements() && !hasAttributeBonuses()
                && cardSlots == 0);
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
}
