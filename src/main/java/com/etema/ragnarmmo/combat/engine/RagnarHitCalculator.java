package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.util.RandomSource;

/**
 * Authoritative central hit calculator for RO-style resolution.
 */
public class RagnarHitCalculator {

    public CombatHitResultType rollBasicAttackHit(double attackerHit, double defenderFlee, RandomSource rng) {
        double hitRate = CombatMath.computeHitRate(attackerHit, defenderFlee);
        if (rng.nextDouble() < hitRate) {
            return CombatHitResultType.HIT;
        }
        return CombatHitResultType.MISS;
    }

    public boolean rollPerfectDodge(double perfectDodgeChance, RandomSource rng) {
        return rng.nextDouble() < perfectDodgeChance;
    }

    public CombatHitResultType rollHitWithCrit(double attackerHit, double defenderFlee, double critChance, double critShield, RandomSource rng) {
        // Perfect Dodge check first
        // (Note: Perfect dodge is usually handled before Hit/Flee in RO)
        
        // Critical check (ignores Flee)
        double finalCrit = Math.max(0.0, critChance - critShield);
        if (rng.nextDouble() < finalCrit) {
            return CombatHitResultType.CRIT;
        }

        // Standard Hit check
        return rollBasicAttackHit(attackerHit, defenderFlee, rng);
    }

    /**
     * Resolves multiple hits into an aggregated result.
     * Returns the number of hits that actually landed.
     */
    public int rollMultiHit(int requestCount, double attackerHit, double defenderFlee, RandomSource rng) {
        int successfulHits = 0;
        for (int i = 0; i < requestCount; i++) {
            if (rollBasicAttackHit(attackerHit, defenderFlee, rng) == CombatHitResultType.HIT) {
                successfulHits++;
            }
        }
        return successfulHits;
    }
}
