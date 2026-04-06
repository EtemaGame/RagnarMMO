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
        
        // Pre-Renewal Hard DEF 50 = 50% Reduction
        // Soft DEF = floor((50+99)/2) + floor(30/5) = 74 + 6 = 80
        // Expected: 1000 * (1 - 0.5) - 80 = 420
        assertEquals(420.0, finalDmg, 0.1);
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
        assertEquals(140.0, critDmg, 0.1); // Strictly 1.4x
    }

    @Test
    public void testStatusAtkBreakpoint() {
        int level = 99;
        int str = 50;
        int dex = 50;
        int luk = 50;
        
        double statusAtk = com.etema.ragnarmmo.system.stats.compute.CombatMath.computeStatusATK(str, dex, luk, level, false);
        
        // STR 50: 50 + 5^2 = 75
        // DEX 50: 50 / 5 = 10
        // LUK 50: floor(50 / 3) = 16
        // LVL 99: 99 * 0.25 = 24.75
        // Total = 75 + 10 + 16 + 24.75 = 125.75
        assertEquals(125.75, statusAtk, 0.01);
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
