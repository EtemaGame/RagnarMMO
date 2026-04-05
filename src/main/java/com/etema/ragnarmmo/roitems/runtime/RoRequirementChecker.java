package com.etema.ragnarmmo.roitems.runtime;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Validates whether a player meets the requirements for an item.
 * Checks base level and job/class restrictions.
 */
public final class RoRequirementChecker {

    private RoRequirementChecker() {}

    /**
     * Result of a requirement check.
     */
    public enum CheckResult {
        /** Player meets all requirements */
        OK,
        /** Player's base level is too low */
        LEVEL_TOO_LOW,
        /** Player's job/class is not allowed */
        WRONG_CLASS,
        /** Could not retrieve player stats (fallback behavior applies) */
        NO_STATS_DATA
    }

    /**
     * Check if a player meets all requirements for an item rule.
     *
     * @param player the player to check
     * @param rule the item rule with requirements
     * @return the check result
     */
    public static CheckResult check(Player player, RoItemRule rule) {
        if (player == null) {
            return CheckResult.NO_STATS_DATA;
        }

        if (rule == null || !rule.hasRequirements()) {
            return CheckResult.OK;
        }

        Optional<IPlayerStats> statsOpt = RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty()) {
            // No stats data available - default to OK to avoid breaking gameplay
            return CheckResult.NO_STATS_DATA;
        }

        IPlayerStats stats = statsOpt.get();

        // Check base level requirement
        if (rule.requiredBaseLevel() > 0) {
            int playerLevel = stats.getLevel();
            if (playerLevel < rule.requiredBaseLevel()) {
                return CheckResult.LEVEL_TOO_LOW;
            }
        }

        // Check class/job requirement
        if (!rule.allowedJobs().isEmpty()) {
            JobType playerJob = JobType.fromId(stats.getJobId());
            boolean jobAllowed = rule.allowedJobs().stream()
                    .anyMatch(playerJob::matchesExactOrAncestor);
            if (!jobAllowed) {
                return CheckResult.WRONG_CLASS;
            }
        }

        return CheckResult.OK;
    }

    /**
     * Convenience method to check if player meets requirements.
     *
     * @param player the player to check
     * @param rule the item rule with requirements
     * @return true if player meets requirements or if check cannot be performed
     */
    public static boolean meetsRequirements(Player player, RoItemRule rule) {
        CheckResult result = check(player, rule);
        // NO_STATS_DATA defaults to allowing (to avoid breaking vanilla gameplay)
        return result == CheckResult.OK || result == CheckResult.NO_STATS_DATA;
    }

    /**
     * Get a human-readable reason for the check result.
     *
     * @param result the check result
     * @param rule the item rule (for context)
     * @return translation key for the message
     */
    public static String getMessageKey(CheckResult result, RoItemRule rule) {
        return switch (result) {
            case LEVEL_TOO_LOW -> "message.ragnarmmo.roitems.level_required";
            case WRONG_CLASS -> "message.ragnarmmo.roitems.class_required";
            case NO_STATS_DATA -> "message.ragnarmmo.roitems.cannot_equip";
            default -> "";
        };
    }

    /**
     * Get the required level for display in messages.
     *
     * @param rule the item rule
     * @return the required level, or 0 if none
     */
    public static int getRequiredLevel(RoItemRule rule) {
        return rule != null ? rule.requiredBaseLevel() : 0;
    }
}
