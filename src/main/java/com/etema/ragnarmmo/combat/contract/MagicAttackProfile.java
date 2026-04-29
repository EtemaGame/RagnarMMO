package com.etema.ragnarmmo.combat.contract;

public record MagicAttackProfile(
        double minMagicAttack,
        double maxMagicAttack) {
    public double averageMagicAttack() {
        return Math.max(0.0D, (minMagicAttack + maxMagicAttack) * 0.5D);
    }
}
