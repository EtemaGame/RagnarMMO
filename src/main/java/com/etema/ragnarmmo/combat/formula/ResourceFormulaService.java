package com.etema.ragnarmmo.combat.formula;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.player.stats.compute.RoPreRenewalFormulaService;

public final class ResourceFormulaService {
    private static final double HP_REGEN_BASE = 1.0;
    private static final double VIT_TO_HP_REGEN = 0.2;
    private static final double HP_REGEN_MAX_PERCENT = 0.02;
    private static final double MANA_REGEN_BASE_PERCENT = 0.01;
    private static final double INT_TO_MANA_REGEN = 0.002;
    private static final double MANA_REGEN_MAX_PERCENT = 0.05;

    private ResourceFormulaService() {
    }

    public static double variableCastSeconds(double baseCast, int dex) {
        return RoPreRenewalFormulaService.variableCastSeconds(baseCast, dex, 0.0D);
    }

    public static double maxHp(int vit, int level, String jobId) {
        double hpBase = 35 + (level * 5 * hpJobMultiplier(jobId));
        return Math.floor(hpBase * (1.0 + vit / 100.0));
    }

    public static double hpRegen(int vit, double maxHp) {
        double regen = HP_REGEN_BASE + vit * VIT_TO_HP_REGEN;
        return Math.min(regen, maxHp * HP_REGEN_MAX_PERCENT);
    }

    public static double maxSp(int intel, int level, String jobId) {
        double spBase = 100 + ((level - 1) * 3 * spJobMultiplier(jobId));
        return Math.floor(spBase * (1.0 + intel / 100.0));
    }

    public static double spRegen(int intel, double maxSp) {
        double regen = maxSp * (MANA_REGEN_BASE_PERCENT + intel * INT_TO_MANA_REGEN);
        return Math.min(regen, maxSp * MANA_REGEN_MAX_PERCENT);
    }

    private static double hpJobMultiplier(String jobId) {
        return switch (JobType.fromId(jobId)) {
            case SWORDSMAN, KNIGHT -> 1.5;
            case THIEF, MERCHANT, ASSASSIN, BLACKSMITH -> 1.2;
            case MAGE, WIZARD -> 0.8;
            default -> 1.0;
        };
    }

    private static double spJobMultiplier(String jobId) {
        return switch (JobType.fromId(jobId)) {
            case MAGE, WIZARD, ACOLYTE, PRIEST -> 1.5;
            case ARCHER, HUNTER -> 1.2;
            case THIEF, ASSASSIN -> 0.8;
            case SWORDSMAN, KNIGHT -> 0.7;
            default -> 1.0;
        };
    }
}
