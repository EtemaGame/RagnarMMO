package com.etema.ragnarmmo.player.stats.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoPreRenewalFormulaServiceTest {
    @Test
    void hitAndFleeAreRoPointValuesNotProbabilities() {
        assertEquals(50.0D, RoPreRenewalFormulaService.hit(40, 10, 0), 0.0001D);
        assertEquals(40.0D, RoPreRenewalFormulaService.flee(30, 10, 0), 0.0001D);
        assertEquals(0.90D, RoPreRenewalFormulaService.hitRate(50, 40), 0.0001D);
    }

    @Test
    void critAndPerfectDodgeAreNormalizedProbabilities() {
        assertEquals(0.30D, RoPreRenewalFormulaService.criticalChance(100, 0), 0.0001D);
        assertEquals(0.10D, RoPreRenewalFormulaService.perfectDodge(100), 0.0001D);
    }

    @Test
    void aspdUsesRoScaleAndConvertsToAttacksPerSecond() {
        assertEquals(5.0D, RoPreRenewalFormulaService.aspdToAttacksPerSecond(190), 0.0001D);
        assertEquals(3.125D, RoPreRenewalFormulaService.aspdToAttacksPerSecond(184), 0.0001D);
    }

    @Test
    void preRenewalVariableCastUsesDexOver150Only() {
        assertEquals(2.0D, RoPreRenewalFormulaService.variableCastSeconds(2.0D, 0, 0), 0.0001D);
        assertEquals(1.0D, RoPreRenewalFormulaService.variableCastSeconds(2.0D, 75, 0), 0.0001D);
        assertEquals(0.0D, RoPreRenewalFormulaService.variableCastSeconds(2.0D, 150, 0), 0.0001D);
    }

    @Test
    void combatMathDelegatesToPreRenewalContract() {
        assertEquals(RoPreRenewalFormulaService.hit(20, 7, 3), CombatMath.computeHIT(20, 99, 7, 3), 0.0001D);
        assertEquals(RoPreRenewalFormulaService.flee(20, 7, 3), CombatMath.computeFLEE(20, 99, 7, 3), 0.0001D);
        assertEquals(RoPreRenewalFormulaService.criticalChance(30, 0.02D), CombatMath.computeCritChance(30, 99, 0.02D), 0.0001D);
        assertEquals(RoPreRenewalFormulaService.perfectDodge(30), CombatMath.computePerfectDodge(30), 0.0001D);
    }
}
