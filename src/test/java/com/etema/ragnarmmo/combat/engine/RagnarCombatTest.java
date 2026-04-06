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
        
        // Hard DEF 50 reduces by ~25-30% depending on formula
        // Soft DEF reduces by flat amount
        assertTrue(finalDmg < rawDmg);
        assertTrue(finalDmg > 0);
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
        // High LUK = High Crit Multiplier
        double critDmg = dmgCalc.applyCriticalModifier(baseDmg, 50, 50);
        assertTrue(critDmg > baseDmg);
        assertEquals(baseDmg * 1.4, critDmg, 0.1); // Based on CombatMath formula (1.4x default roughly)
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
