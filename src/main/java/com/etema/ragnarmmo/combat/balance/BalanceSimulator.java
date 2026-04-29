package com.etema.ragnarmmo.combat.balance;

import com.etema.ragnarmmo.combat.engine.RagnarDamageCalculator;

public final class BalanceSimulator {
    private static final RagnarDamageCalculator DAMAGE_CALCULATOR = new RagnarDamageCalculator();

    private BalanceSimulator() {
    }

    public static double expectedPhysicalDamage(BalancePlayerFixture attacker, BalanceMobFixture defender) {
        double damage = attacker.averageAttack();
        damage = DAMAGE_CALCULATOR.applyPhysicalDefense(
                damage,
                Math.max(0, defender.level() / 3),
                Math.max(0, defender.flee() - defender.level()),
                attacker.level(),
                defender.def());
        return Math.max(1.0D, damage);
    }

    public static double expectedMagicDamage(BalancePlayerFixture attacker, BalanceMobFixture defender) {
        double damage = attacker.averageMagicAttack();
        damage = DAMAGE_CALCULATOR.applyMagicDefense(
                damage,
                Math.max(0, defender.level() / 4),
                Math.max(0, defender.level() / 4),
                Math.max(0, defender.flee() - defender.level()),
                defender.level(),
                defender.mdef());
        return Math.max(1.0D, damage);
    }

    public static int hitsToKill(double damagePerHit, int hp) {
        return (int) Math.ceil(Math.max(1, hp) / Math.max(1.0D, damagePerHit));
    }
}
