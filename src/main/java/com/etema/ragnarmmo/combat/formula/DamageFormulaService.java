package com.etema.ragnarmmo.combat.formula;

import java.util.Random;

public final class DamageFormulaService {
    private static final double DEX_TO_ATK_DIVISOR = 5.0;
    private static final double STR_TO_RANGED_ATK_DIVISOR = 5.0;
    private static final double LUK_TO_ATK_DIVISOR = 5.0;
    private static final double MIN_DAMAGE_ROLL = 0.8;
    private static final double DEX_VARIANCE_DIVISOR = 150.0;
    private static final double LUK_VARIANCE_BONUS = 300.0;
    private static final double CRIT_BASE_MULT = 1.4;

    private DamageFormulaService() {
    }

    public static double statusAtk(int str, int dex, int luk, boolean ranged) {
        if (ranged) {
            return dex
                    + Math.pow(Math.floor(dex / 10.0), 2)
                    + Math.floor(str / STR_TO_RANGED_ATK_DIVISOR)
                    + Math.floor(luk / LUK_TO_ATK_DIVISOR);
        }
        return str
                + Math.pow(Math.floor(str / 10.0), 2)
                + Math.floor(dex / DEX_TO_ATK_DIVISOR)
                + Math.floor(luk / LUK_TO_ATK_DIVISOR);
    }

    public static double weaponAtk(double weaponBase) {
        return Math.max(0.0, weaponBase);
    }

    public static int rangedDrawTicks(int baseDrawTicks, int agi) {
        double reduction = Math.min(0.9, agi / 100.0);
        return (int) Math.max(1, Math.round(baseDrawTicks * (1.0 - reduction)));
    }

    public static double totalAtk(int str, int dex, int luk, double weaponAtk, double bonusAtk, boolean ranged) {
        return statusAtk(str, dex, luk, ranged) + weaponAtk(weaponAtk) + bonusAtk;
    }

    public static double damageVariance(double baseDamage, int dex, int luk, Random rng) {
        double floor = damageVarianceFloorMultiplier(dex, luk);
        return baseDamage * (floor + rng.nextDouble() * (1.0 - floor));
    }

    public static double damageVarianceFloor(double baseDamage, int dex, int luk) {
        return baseDamage * damageVarianceFloorMultiplier(dex, luk);
    }

    public static double statusMatkMin(int intel) {
        return intel + Math.pow(Math.floor(intel / 7.0), 2);
    }

    public static double statusMatkMax(int intel) {
        return intel + Math.pow(Math.floor(intel / 5.0), 2);
    }

    public static double statusMatk(int intel) {
        return (statusMatkMin(intel) + statusMatkMax(intel)) * 0.5;
    }

    public static double totalMatk(int intel, double spellBase, double bonusMatk) {
        return statusMatk(intel) + spellBase + bonusMatk;
    }

    public static double critDamageMultiplier() {
        return CRIT_BASE_MULT;
    }

    private static double damageVarianceFloorMultiplier(int dex, int luk) {
        double dexFactor = FormulaUtil.clamp(0, 1, dex / DEX_VARIANCE_DIVISOR);
        double lukBonus = LUK_VARIANCE_BONUS > 0 ? luk / LUK_VARIANCE_BONUS : 0;
        return FormulaUtil.clamp(MIN_DAMAGE_ROLL, 1.0, MIN_DAMAGE_ROLL + (1 - MIN_DAMAGE_ROLL) * (dexFactor + lukBonus));
    }
}
