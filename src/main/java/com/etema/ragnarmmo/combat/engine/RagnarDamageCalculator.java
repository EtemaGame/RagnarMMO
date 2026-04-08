package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

/**
 * Authoritative centralized damage calculator for RO-style combat.
 */
public class RagnarDamageCalculator {

    public double computePhysicalDamage(double baseAttack, int dex, int luk, Random rng) {
        return CombatMath.computeDamageVariance(baseAttack, dex, luk, rng);
    }

    public double applyPhysicalDefense(double damage, int vit, int agi, int attackerLevel, double armorEff) {
        double softDEF = CombatMath.computeSoftDEF(vit, agi, attackerLevel);
        double hardDEF = CombatMath.computeHardDEF(armorEff, vit);
        double drPhys = CombatMath.computePhysDR(hardDEF);
        return CombatMath.applyPhysicalDefense(damage, softDEF, hardDEF, drPhys);
    }

    public double applyMagicDefense(double damage, int intel, int vit, int dex, int level, double equipMDEF) {
        double softMdef = CombatMath.computeSoftMDEF(intel, vit);
        double hardMdef = CombatMath.computeHardMDEF(equipMDEF);
        return CombatMath.applyMagicDefense(damage, softMdef, hardMdef);
    }

    public double applyCriticalModifier(double amount, int luk, int str) {
        double critMult = CombatMath.computeCritDamageMultiplier(luk, str);
        return amount * critMult;
    }

    public double applyModifiers(double damage, ItemStack weapon, LivingEntity target, ElementType attackElement, boolean isMagic) {
        double modifier = 1.0;
        
        // Size Penalty (Physical only)
        if (!isMagic) {
            CombatMath.MobSize size = CombatPropertyResolver.getEntitySize(target);
            modifier *= CombatMath.getWeaponSizePenalty(weapon, size);
        }

        // Elemental Modifier
        ElementType defenseElement = CombatPropertyResolver.getDefensiveElement(target);
        modifier *= CombatPropertyResolver.getElementalModifier(attackElement, defenseElement);

        return damage * modifier;
    }
}
