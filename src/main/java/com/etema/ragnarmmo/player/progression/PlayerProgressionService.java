package com.etema.ragnarmmo.player.progression;

import net.minecraft.resources.ResourceLocation;

public final class PlayerProgressionService {
    private static final ResourceLocation DEFAULT_JOB_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "novice");

    private final ProgressionRules rules;

    public PlayerProgressionService(ProgressionRules rules) {
        this.rules = rules;
    }

    public static PlayerProgressionService forJobId(ResourceLocation jobId) {
        return new PlayerProgressionService(ProgressionRules.currentFromConfig(jobId == null ? DEFAULT_JOB_ID : jobId));
    }

    public ProgressionResult addBaseExp(PlayerProgression current, long amount) {
        if (amount <= 0) {
            return unchanged(current);
        }
        int level = Math.min(current.baseLevel(), rules.maxBaseLevel());
        long exp = Math.max(0L, current.baseExp());
        int statPoints = Math.max(0, current.statPoints());
        int levelsGained = 0;
        int statPointsAwarded = 0;

        if (level >= rules.maxBaseLevel()) {
            return new ProgressionResult(
                    new PlayerProgression(rules.maxBaseLevel(), 0L, current.jobLevel(), current.jobExp(),
                            statPoints, current.skillPoints(), sanitizeJobId(current.jobId())),
                    0,
                    0,
                    0,
                    0,
                    true,
                    current.jobLevel() >= rules.maxJobLevel());
        }

        exp += amount;
        while (level < rules.maxBaseLevel()) {
            int required = rules.baseLevelCurve().expToNext(level);
            if (exp < required) {
                break;
            }
            exp -= required;
            level++;
            levelsGained++;
            int awarded = rules.statPointsForLevelUp(level);
            statPoints += awarded;
            statPointsAwarded += awarded;
        }

        boolean reachedCap = level >= rules.maxBaseLevel();
        if (reachedCap) {
            exp = 0L;
        }

        return new ProgressionResult(
                new PlayerProgression(level, exp, current.jobLevel(), current.jobExp(), statPoints,
                        current.skillPoints(), sanitizeJobId(current.jobId())),
                levelsGained,
                0,
                statPointsAwarded,
                0,
                reachedCap,
                current.jobLevel() >= rules.maxJobLevel());
    }

    public ProgressionResult addJobExp(PlayerProgression current, long amount) {
        if (amount <= 0) {
            return unchanged(current);
        }
        int jobLevel = Math.min(current.jobLevel(), rules.maxJobLevel());
        long jobExp = Math.max(0L, current.jobExp());
        int skillPoints = Math.max(0, current.skillPoints());
        int levelsGained = 0;

        if (jobLevel >= rules.maxJobLevel()) {
            return new ProgressionResult(
                    new PlayerProgression(current.baseLevel(), current.baseExp(), rules.maxJobLevel(), 0L,
                            current.statPoints(), skillPoints, sanitizeJobId(current.jobId())),
                    0,
                    0,
                    0,
                    0,
                    current.baseLevel() >= rules.maxBaseLevel(),
                    true);
        }

        jobExp += amount;
        while (jobLevel < rules.maxJobLevel()) {
            int required = rules.jobLevelCurve().expToNext(jobLevel);
            if (jobExp < required) {
                break;
            }
            jobExp -= required;
            jobLevel++;
            levelsGained++;
            skillPoints++;
        }

        boolean reachedCap = jobLevel >= rules.maxJobLevel();
        if (reachedCap) {
            jobExp = 0L;
        }

        return new ProgressionResult(
                new PlayerProgression(current.baseLevel(), current.baseExp(), jobLevel, jobExp,
                        current.statPoints(), skillPoints, sanitizeJobId(current.jobId())),
                0,
                levelsGained,
                0,
                levelsGained,
                current.baseLevel() >= rules.maxBaseLevel(),
                reachedCap);
    }

    public int baseExpToNext(int level) {
        return rules.baseLevelCurve().expToNext(level);
    }

    public int jobExpToNext(int level) {
        return rules.jobLevelCurve().expToNext(level);
    }

    public int applyBaseExpRate(int rawExp) {
        return rules.applyBaseExpRate(rawExp);
    }

    public int applyJobExpRate(int rawExp) {
        return rules.applyJobExpRate(rawExp);
    }

    public int computeBaseDeathPenaltyLoss(long currentExp) {
        return rules.computeBaseDeathPenaltyLoss(currentExp);
    }

    public int computeJobDeathPenaltyLoss(long currentExp) {
        return rules.computeJobDeathPenaltyLoss(currentExp);
    }

    public float getBaseProgressPercent(long currentExp, int level) {
        int needed = baseExpToNext(level);
        if (needed <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, (float) currentExp / needed);
    }

    public float getJobProgressPercent(long currentExp, int jobLevel) {
        int needed = jobExpToNext(jobLevel);
        if (needed <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, (float) currentExp / needed);
    }

    private static ProgressionResult unchanged(PlayerProgression current) {
        return new ProgressionResult(
                current,
                0,
                0,
                0,
                0,
                false,
                false);
    }

    private static ResourceLocation sanitizeJobId(ResourceLocation jobId) {
        return jobId == null ? DEFAULT_JOB_ID : jobId;
    }
}
