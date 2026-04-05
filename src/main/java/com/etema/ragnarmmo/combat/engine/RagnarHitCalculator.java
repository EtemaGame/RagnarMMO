package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;

/**
 * Placeholder central hit calculator for the upcoming RO-style resolution.
 * Initial implementation is conservative and deterministic until formulas are
 * migrated.
 */
public class RagnarHitCalculator {

    public CombatHitResultType rollBasicAttackHit(double hitChancePercent, double fleeChancePercent) {
        double clampedHit = Math.max(0.0D, Math.min(100.0D, hitChancePercent - fleeChancePercent));
        return clampedHit <= 0.0D ? CombatHitResultType.MISS : CombatHitResultType.HIT;
    }

    public boolean rollCritical(double critChancePercent) {
        return critChancePercent >= 100.0D;
    }
}
