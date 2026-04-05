package com.etema.ragnarmmo.combat.engine;

/**
 * Placeholder centralized damage calculator. The goal is to migrate all shared
 * physical/magical formula paths here.
 */
public class RagnarDamageCalculator {

    public double computePhysicalDamage(double baseAttack, double damageMultiplier, double flatReduction) {
        return Math.max(0.0D, (baseAttack * Math.max(0.0D, damageMultiplier)) - Math.max(0.0D, flatReduction));
    }

    public double computeMagicDamage(double baseMagic, double damageMultiplier, double flatReduction) {
        return Math.max(0.0D, (baseMagic * Math.max(0.0D, damageMultiplier)) - Math.max(0.0D, flatReduction));
    }

    public double applyCriticalModifier(double amount, double critMultiplier) {
        return Math.max(0.0D, amount * Math.max(1.0D, critMultiplier));
    }
}
