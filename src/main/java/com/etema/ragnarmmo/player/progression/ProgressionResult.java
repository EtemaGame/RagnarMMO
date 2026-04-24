package com.etema.ragnarmmo.player.progression;

public record ProgressionResult(
        PlayerProgression progression,
        int baseLevelsGained,
        int jobLevelsGained,
        int statPointsAwarded,
        int skillPointsAwarded,
        boolean reachedBaseCap,
        boolean reachedJobCap) {
}
