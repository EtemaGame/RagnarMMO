package com.etema.ragnarmmo.combat.formula;

public final class StatusResistanceService {
    private StatusResistanceService() {
    }

    public static float chanceByVitAndLuk(float baseChance, int vit, int luk) {
        return (float) (baseChance * FormulaUtil.clamp(0.0, 1.0, 1.0 - (vit / 100.0) - (luk / 300.0)));
    }

    public static int durationByVit(int baseDurationTicks, int vit) {
        return (int) (baseDurationTicks * FormulaUtil.clamp(0.0, 1.0, 1.0 - (vit / 100.0)));
    }

    public static float chanceByIntAndLuk(float baseChance, int intel, int luk) {
        return (float) (baseChance * FormulaUtil.clamp(0.0, 1.0, 1.0 - (intel / 100.0) - (luk / 300.0)));
    }

    public static int durationByInt(int baseDurationTicks, int intel) {
        return (int) (baseDurationTicks * FormulaUtil.clamp(0.0, 1.0, 1.0 - (intel / 100.0)));
    }

    public static float chanceByMdefAndLuk(float baseChance, int mdef, int luk) {
        return (float) (baseChance * FormulaUtil.clamp(0.0, 1.0, 1.0 - (mdef / 100.0) - (luk / 300.0)));
    }

    public static float chanceByAgiAndLuk(float baseChance, int agi, int luk) {
        return (float) (baseChance * FormulaUtil.clamp(0.0, 1.0, 1.0 - (agi / 100.0) - (luk / 300.0)));
    }

    public static int durationByAgi(int baseDurationTicks, int agi) {
        return (int) (baseDurationTicks * FormulaUtil.clamp(0.0, 1.0, 1.0 - (agi / 100.0)));
    }
}
