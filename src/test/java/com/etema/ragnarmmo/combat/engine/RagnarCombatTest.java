package com.etema.ragnarmmo.combat.engine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import net.minecraft.util.RandomSource;

public class RagnarCombatTest {

    private final RagnarHitCalculator hitCalc = new RagnarHitCalculator();
    private final RagnarDamageCalculator dmgCalc = new RagnarDamageCalculator();

    @Test
    public void testPhysicalDamageVariance() {
        double baseAtk = 100.0;
        int dex = 50;
        int luk = 30;
        RandomSource rng = RandomSource.create(12345);
        
        double dmg1 = dmgCalc.computePhysicalDamage(baseAtk, dex, luk, new Random(12345));
        double dmg2 = dmgCalc.computePhysicalDamage(baseAtk, dex, luk, new Random(12346));
        
        // Damage should be around baseAtk but with some variance based on DEX/LUK
        assertTrue(dmg1 > 0);
        assertNotEquals(dmg1, dmg2); // Since it's random
    }

    @Test
    public void testPhysicalDefense() {
        double rawDmg = 1000.0;
        int vit = 50;
        int agi = 30;
        int level = 99;
        double armorEff = 50.0; // 50 Hard DEF

        double finalDmg = dmgCalc.applyPhysicalDefense(rawDmg, vit, agi, level, armorEff);
        System.out.println("DEBUG: testPhysicalDefense - raw=" + rawDmg + ", vit=" + vit + ", agi=" + agi + ", lvl=" + level + ", armor=" + armorEff + " -> finalDmg=" + finalDmg);
        
        // Pre-Renewal Hard DEF 50 = 50% reduction
        // Soft DEF = floor(50 * 0.5) + max(floor(50 * 0.3), floor(50^2 / 150) - 1) = 25 + 15 = 40
        // Expected: 1000 * (1 - 0.5) - 40 = 460
        assertEquals(460.0, finalDmg, 0.1, "finalDmg calculation failure: expected 460.0 but got " + finalDmg);
    }

    @Test
    public void testHitAccuracy() {
        double attackerHit = 200.0;
        double defenderFlee = 150.0;
        RandomSource rng = RandomSource.create(1);
        
        // 200 vs 150 should have a decent hit chance
        CombatHitResultType result = hitCalc.rollHitWithCrit(attackerHit, defenderFlee, 0, 0, rng);
        assertNotEquals(CombatHitResultType.MISS, result);
    }

    @Test
    public void testCriticalHit() {
        double baseDmg = 100.0;
        // High LUK should NOT increase multiplier in Classic Pre-Renewal
        double critDmg = dmgCalc.applyCriticalModifier(baseDmg, 50, 50);
        System.out.println("DEBUG: testCriticalHit - base=" + baseDmg + " -> critDmg=" + critDmg);
        assertEquals(140.0, critDmg, 0.1, "critDmg calculation failure: expected 140.0 but got " + critDmg); // Strictly 1.4x
    }

    @Test
    public void testStatusAtkBreakpoint() {
        int level = 99;
        int str = 50;
        int dex = 50;
        int luk = 50;
        
        double statusAtk = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStatusATK(str, dex, luk, level, false);
        
        // STR 50: 50 + 5^2 = 75
        // DEX contribution: floor(50 / 5) = 10
        // LUK contribution: floor(50 / 5) = 10
        // Total = 95
        assertEquals(95.0, statusAtk, 0.01);
    }

    @Test
    public void testBowStatusAtkUsesDexPrimary() {
        double statusAtk = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStatusATK(20, 60, 25, 99, true);

        // DEX 60: 60 + 6^2 = 96
        // STR contribution: floor(20 / 5) = 4
        // LUK contribution: floor(25 / 5) = 5
        assertEquals(105.0, statusAtk, 0.01);
    }

    @Test
    public void testMagicAttackRangeUsesIntOnly() {
        assertEquals(170.0, com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStatusMATKMin(70), 0.01);
        assertEquals(266.0, com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStatusMATKMax(70), 0.01);
    }

    @Test
    public void testCastTimeUsesDexOnly() {
        double castTime = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeCastTime(2.0, 75, 1, false);
        assertEquals(1.0, castTime, 0.01);
    }

    @Test
    public void testMagicDefenseAppliesHardThenSoft() {
        double finalDmg = com.etema.ragnarmmo.system.stats.compute.CombatMath.applyMagicDefense(100.0, 30.0, 20.0);
        assertEquals(50.0, finalDmg, 0.01);
    }

    @Test
    public void testMultiHit() {
        int hitCount = 3;
        double attackerHit = 200.0;
        double minHitChance = 50.0;
        RandomSource rng = RandomSource.create(42);
        
        int landed = hitCalc.rollMultiHit(hitCount, attackerHit, minHitChance, rng);
        
        // With high hit (200), we should land most hits
        assertTrue(landed >= 1 && landed <= hitCount);
    }
}
