package com.etema.ragnarmmo.combat.formula;

public final class FormulaUtil {
    private FormulaUtil() {
    }

    public static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    public static double soft(double value, double constant) {
        return value / (value + constant);
    }
}
