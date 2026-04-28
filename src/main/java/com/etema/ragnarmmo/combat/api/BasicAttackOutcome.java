package com.etema.ragnarmmo.combat.api;

import java.util.List;

public record BasicAttackOutcome(
        BasicAttackSource source,
        boolean accepted,
        boolean shouldCancelVanilla,
        CombatRejectReason rejectReason,
        BasicAttackFailureReason failureReason,
        List<CombatResolution> resolutions,
        List<ResolvedTargetCandidate> targetResults,
        boolean fallbackUsed) {

    public BasicAttackOutcome {
        source = source == null ? BasicAttackSource.SERVER_ATTACK_EVENT : source;
        failureReason = failureReason == null ? BasicAttackFailureReason.NONE : failureReason;
        resolutions = resolutions == null ? List.of() : List.copyOf(resolutions);
        targetResults = targetResults == null ? List.of() : List.copyOf(targetResults);
    }

    public boolean rejected() {
        return !accepted;
    }

    public static BasicAttackOutcome rejected(
            BasicAttackSource source,
            CombatRejectReason reason,
            boolean cancelVanilla,
            List<ResolvedTargetCandidate> targetResults) {
        return new BasicAttackOutcome(
                source,
                false,
                cancelVanilla,
                reason,
                BasicAttackFailureReason.NONE,
                List.of(),
                targetResults,
                false);
    }

    public static BasicAttackOutcome resolved(
            BasicAttackSource source,
            List<CombatResolution> resolutions,
            List<ResolvedTargetCandidate> targetResults,
            boolean fallbackUsed) {
        return new BasicAttackOutcome(
                source,
                true,
                true,
                null,
                BasicAttackFailureReason.NONE,
                resolutions,
                targetResults,
                fallbackUsed);
    }

    public static BasicAttackOutcome infrastructureFallback(
            BasicAttackSource source,
            BasicAttackFailureReason reason,
            CombatResolution fallbackResolution,
            List<ResolvedTargetCandidate> targetResults) {
        return new BasicAttackOutcome(
                source,
                true,
                true,
                null,
                reason,
                fallbackResolution == null ? List.of() : List.of(fallbackResolution),
                targetResults,
                fallbackResolution != null);
    }

    public BasicAttackOutcome withFailure(BasicAttackFailureReason reason) {
        return new BasicAttackOutcome(
                source,
                accepted,
                shouldCancelVanilla,
                rejectReason,
                reason,
                resolutions,
                targetResults,
                fallbackUsed);
    }
}
