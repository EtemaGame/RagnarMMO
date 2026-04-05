package com.etema.ragnarmmo.system.stats.progression;

import com.etema.ragnarmmo.common.config.RagnarConfigs;

import java.util.function.IntUnaryOperator;

/**
 * ExpTable - Experience curves using hardcoded quadratic formulas.
 * Global multipliers are applied from RagnarConfigs.
 */
public final class ExpTable {

    // Hardcoded EXP curve constants (previously in BalanceConfig)
    private static final int EXP_MINIMUM = 50;

    private static final int JOB_EXP_MINIMUM = 25;

    public static int expToNext(int level) {
        int result = applyCustomCurve(level, null, ExperienceHooks::getExpCurve);
        if (result > 0) {
            return result;
        }

        // RO-inspired formula: 100 * 1.15^(level-1) + level^4 * 0.05
        int expRequired = (int) Math.round(100.0 * Math.pow(1.15, Math.max(0, level - 1)) + Math.pow(level, 4) * 0.05);
        return Math.max(EXP_MINIMUM, expRequired);
    }

    public static int jobExpToNext(int jobLevel) {
        int result = applyCustomCurve(jobLevel, null, ExperienceHooks::getJobExpCurve);
        if (result > 0) {
            return result;
        }

        // RO-inspired Job formula: 50 * 1.12^(level-1) + level^3.5 * 0.04
        int expRequired = (int) Math
                .round(50.0 * Math.pow(1.12, Math.max(0, jobLevel - 1)) + Math.pow(jobLevel, 3.5) * 0.04);
        return Math.max(JOB_EXP_MINIMUM, expRequired);
    }

    public static long getTotalExpForLevel(int targetLevel) {
        long total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) {
            total += expToNext(lvl);
        }
        return total;
    }

    public static int getLevelFromTotalExp(long totalExp) {
        int level = 1;
        long accumulated = 0;

        int maxLevel = RagnarConfigs.SERVER.caps.maxLevel.get();
        while (accumulated < totalExp && level < maxLevel) {
            int needed = expToNext(level);
            if (accumulated + needed > totalExp) {
                break;
            }
            accumulated += needed;
            level++;
        }

        return level;
    }

    public static float getProgressPercent(int currentExp, int level) {
        int needed = expToNext(level);
        if (needed <= 0)
            return 1.0f;
        return Math.min(1.0f, (float) currentExp / needed);
    }

    public static float getJobProgressPercent(int currentExp, int jobLevel) {
        int needed = jobExpToNext(jobLevel);
        if (needed <= 0)
            return 1.0f;
        return Math.min(1.0f, (float) currentExp / needed);
    }

    private static int applyCustomCurve(int level, String curveId,
            java.util.function.Function<String, IntUnaryOperator> resolver) {
        if (curveId != null) {
            IntUnaryOperator operator = resolver.apply(curveId);
            if (operator != null) {
                return Math.max(1, operator.applyAsInt(level));
            }
        }
        return -1;
    }
}
