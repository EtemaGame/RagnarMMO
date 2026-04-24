package com.etema.ragnarmmo.player.progression;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.resources.ResourceLocation;

public record ProgressionRules(
        int maxBaseLevel,
        int maxJobLevel,
        int baseStatPoints,
        int pointsPerLevel,
        boolean usePreRenewalStatPointCurve,
        double baseExpRate,
        double jobExpRate,
        double baseDeathPenaltyRate,
        double jobDeathPenaltyRate,
        LevelCurve baseLevelCurve,
        LevelCurve jobLevelCurve) {

    public static ProgressionRules current(ResourceLocation jobId) {
        JobType jobType = JobType.fromId(jobId == null ? "ragnarmmo:novice" : jobId.toString());
        boolean novice = jobType == JobType.NOVICE;
        return new ProgressionRules(
                novice ? configInt(() -> RagnarConfigs.SERVER.caps.noviceMaxLevel.get(), 10)
                        : configInt(() -> RagnarConfigs.SERVER.caps.maxLevel.get(), 99),
                novice ? configInt(() -> RagnarConfigs.SERVER.caps.noviceMaxJobLevel.get(), 10)
                        : configInt(() -> RagnarConfigs.SERVER.caps.maxJobLevel.get(), 50),
                configInt(() -> RagnarConfigs.SERVER.progression.baseStatPoints.get(), 48),
                configInt(() -> RagnarConfigs.SERVER.progression.pointsPerLevel.get(), 3),
                configBoolean(() -> RagnarConfigs.SERVER.progression.usePreRenewalStatPointCurve.get(), true),
                configDouble(() -> RagnarConfigs.SERVER.progression.expGlobalMultiplier.get(), 1.0D),
                configDouble(() -> RagnarConfigs.SERVER.progression.jobExpGlobalMultiplier.get(), 1.0D),
                configDouble(() -> RagnarConfigs.SERVER.progression.baseExpDeathPenaltyRate.get(), 0.05D),
                configDouble(() -> RagnarConfigs.SERVER.progression.jobExpDeathPenaltyRate.get(), 0.05D),
                new FormulaLevelCurve(50, 100.0, 1.15, 4.0, 0.05),
                new FormulaLevelCurve(25, 50.0, 1.12, 3.5, 0.04));
    }

    public int statPointsForLevelUp(int newBaseLevel) {
        if (!usePreRenewalStatPointCurve) {
            return Math.max(0, pointsPerLevel);
        }
        return Math.max(0, (Math.max(1, newBaseLevel) / 5) + 3);
    }

    public int applyBaseExpRate(int rawExp) {
        return applyGainMultiplier(rawExp, baseExpRate);
    }

    public int applyJobExpRate(int rawExp) {
        return applyGainMultiplier(rawExp, jobExpRate);
    }

    public int computeBaseDeathPenaltyLoss(long currentExp) {
        return computeDeathPenaltyLoss(currentExp, baseDeathPenaltyRate);
    }

    public int computeJobDeathPenaltyLoss(long currentExp) {
        return computeDeathPenaltyLoss(currentExp, jobDeathPenaltyRate);
    }

    private static int applyGainMultiplier(int rawExp, double multiplier) {
        if (rawExp <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(rawExp * multiplier));
    }

    private static int computeDeathPenaltyLoss(long currentExp, double penaltyRate) {
        if (currentExp <= 0 || penaltyRate <= 0.0) {
            return 0;
        }
        return Math.max(0, (int) Math.round(currentExp * penaltyRate));
    }

    private static int configInt(java.util.function.IntSupplier supplier, int fallbackValue) {
        try {
            return supplier.getAsInt();
        } catch (IllegalStateException ex) {
            return fallbackValue;
        }
    }

    private static double configDouble(java.util.function.DoubleSupplier supplier, double fallbackValue) {
        try {
            return supplier.getAsDouble();
        } catch (IllegalStateException ex) {
            return fallbackValue;
        }
    }

    private static boolean configBoolean(java.util.function.BooleanSupplier supplier, boolean fallbackValue) {
        try {
            return supplier.getAsBoolean();
        } catch (IllegalStateException ex) {
            return fallbackValue;
        }
    }
}
