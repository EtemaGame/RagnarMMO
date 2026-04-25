package com.etema.ragnarmmo.player.progression;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

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

    public ProgressionRules {
        if (maxBaseLevel < 1) throw new IllegalArgumentException("maxBaseLevel must be >= 1");
        if (maxJobLevel < 1) throw new IllegalArgumentException("maxJobLevel must be >= 1");
        if (baseStatPoints < 0) throw new IllegalArgumentException("baseStatPoints must be >= 0");
        if (pointsPerLevel < 0) throw new IllegalArgumentException("pointsPerLevel must be >= 0");
        if (baseExpRate < 0.0D || jobExpRate < 0.0D) throw new IllegalArgumentException("exp rates must be >= 0");
        if (baseDeathPenaltyRate < 0.0D || jobDeathPenaltyRate < 0.0D) {
            throw new IllegalArgumentException("death penalty rates must be >= 0");
        }
        Objects.requireNonNull(baseLevelCurve, "baseLevelCurve");
        Objects.requireNonNull(jobLevelCurve, "jobLevelCurve");
    }

    public static ProgressionRules currentFromConfig(ResourceLocation jobId) {
        JobType jobType = JobType.fromId(jobId == null ? "ragnarmmo:novice" : jobId.toString());
        boolean novice = jobType == JobType.NOVICE;
        return new ProgressionRules(
                novice ? RagnarConfigs.SERVER.caps.noviceMaxLevel.get() : RagnarConfigs.SERVER.caps.maxLevel.get(),
                novice ? RagnarConfigs.SERVER.caps.noviceMaxJobLevel.get() : RagnarConfigs.SERVER.caps.maxJobLevel.get(),
                RagnarConfigs.SERVER.progression.baseStatPoints.get(),
                RagnarConfigs.SERVER.progression.pointsPerLevel.get(),
                RagnarConfigs.SERVER.progression.usePreRenewalStatPointCurve.get(),
                RagnarConfigs.SERVER.progression.expGlobalMultiplier.get(),
                RagnarConfigs.SERVER.progression.jobExpGlobalMultiplier.get(),
                RagnarConfigs.SERVER.progression.baseExpDeathPenaltyRate.get(),
                RagnarConfigs.SERVER.progression.jobExpDeathPenaltyRate.get(),
                new FormulaLevelCurve(50, 100.0, 1.15, 4.0, 0.05),
                new FormulaLevelCurve(25, 50.0, 1.12, 3.5, 0.04));
    }

    public static ProgressionRules defaultsForTests(ResourceLocation jobId) {
        JobType jobType = JobType.fromId(jobId == null ? "ragnarmmo:novice" : jobId.toString());
        boolean novice = jobType == JobType.NOVICE;
        return new ProgressionRules(
                novice ? 10 : 99,
                novice ? 10 : 50,
                48,
                3,
                true,
                1.0D,
                1.0D,
                0.05D,
                0.05D,
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

}
