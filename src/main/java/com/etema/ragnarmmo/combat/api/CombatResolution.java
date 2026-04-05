package com.etema.ragnarmmo.combat.api;

/**
 * Canonical result payload produced by the combat engine.
 */
public record CombatResolution(
        int attackerId,
        int targetId,
        CombatHitResultType resultType,
        double baseAmount,
        double finalAmount,
        boolean critical,
        boolean missed) {

    public static CombatResolution miss(int attackerId, int targetId) {
        return new CombatResolution(attackerId, targetId, CombatHitResultType.MISS, 0.0D, 0.0D, false, true);
    }

    public static CombatResolution hit(int attackerId, int targetId, double baseAmount, double finalAmount,
            boolean critical) {
        return new CombatResolution(attackerId, targetId,
                critical ? CombatHitResultType.CRIT : CombatHitResultType.HIT,
                baseAmount,
                finalAmount,
                critical,
                false);
    }
}
