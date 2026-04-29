package com.etema.ragnarmmo.combat.contract;

import net.minecraft.world.item.ItemStack;

public record PhysicalAttackProfile(
        double minAttack,
        double maxAttack,
        double hit,
        double critChance,
        double critDamageMultiplier,
        int aspdRo,
        ItemStack weapon) {
    public PhysicalAttackProfile {
        weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
    }

    public double averageAttack() {
        return Math.max(0.0D, (minAttack + maxAttack) * 0.5D);
    }
}
