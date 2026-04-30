package com.etema.ragnarmmo.player.stats.compute;

/**
 * Source of truth for Ragnarok Online pre-renewal formulas used by RagnarMMO.
 *
 * <p>Unit contract:
 * <ul>
 *     <li>HIT and FLEE are RO point values, not probabilities.</li>
 *     <li>Critical chance and perfect dodge are normalized probabilities in 0.0..1.0.</li>
 *     <li>ASPD is represented in RO's 0..190 scale, then converted to attacks per second.</li>
 *     <li>Cast times are expressed in seconds at this layer; callers convert to ticks.</li>
 * </ul>
 */
public final class RoPreRenewalFormulaService {
    public static final double HIT_BASE = 0.0D;
    public static final double FLEE_BASE = 0.0D;
    public static final double HIT_FLEE_FORMULA_CONSTANT = 80.0D;
    public static final double MIN_HIT_RATE = 0.05D;
    public static final double MAX_HIT_RATE = 0.95D;
    public static final double ASPD_RO_MIN = 50.0D;
    public static final double ASPD_RO_MAX = 190.0D;
    public static final double APS_MIN = 0.25D;
    public static final double APS_MAX = 5.0D;

    private RoPreRenewalFormulaService() {
    }

    public static double hit(int dex, int level, double bonus) {
        return HIT_BASE + Math.max(0, level) + Math.max(0, dex) + bonus;
    }

    public static double flee(int agi, int level, double bonus) {
        return FLEE_BASE + Math.max(0, level) + Math.max(0, agi) + bonus;
    }

    public static double hitRate(double attackerHit, double defenderFlee) {
        return clamp(MIN_HIT_RATE, MAX_HIT_RATE,
                (HIT_FLEE_FORMULA_CONSTANT + attackerHit - defenderFlee) / 100.0D);
    }

    public static double criticalChance(int luk, double bonus) {
        return clamp(0.0D, 1.0D, ((Math.max(0, luk) * 0.3D) / 100.0D) + bonus);
    }

    public static double perfectDodge(int luk) {
        return clamp(0.0D, 1.0D, (Math.max(0, luk) * 0.1D) / 100.0D);
    }

    public static int aspdRo(int baseWeaponAspd, boolean hasShield, int agi, int dex, double bonus) {
        double aspd = baseWeaponAspd
                + Math.max(0, agi) * 0.25D
                + Math.max(0, dex) * 0.1D
                + bonus;
        if (hasShield) {
            aspd -= CombatMath.SHIELD_ASPD_PENALTY;
        }
        return (int) clamp(ASPD_RO_MIN, ASPD_RO_MAX, aspd);
    }

    public static double aspdToAttacksPerSecond(int aspdRo) {
        if (aspdRo >= ASPD_RO_MAX) {
            return APS_MAX;
        }
        if (aspdRo <= 0) {
            return APS_MIN;
        }
        return clamp(APS_MIN, APS_MAX, 50.0D / (200.0D - aspdRo));
    }

    public static double variableCastSeconds(double baseCastSeconds, int dex, double modifierPercent) {
        double dexFactor = 1.0D - clamp(0.0D, 1.0D, Math.max(0, dex) / 150.0D);
        double modifierFactor = 1.0D - clamp(0.0D, 1.0D, modifierPercent / 100.0D);
        return Math.max(0.0D, baseCastSeconds * dexFactor * modifierFactor);
    }

    static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }
}
