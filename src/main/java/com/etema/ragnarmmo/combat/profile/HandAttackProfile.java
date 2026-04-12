package com.etema.ragnarmmo.combat.profile;

import net.minecraft.world.item.ItemStack;

public record HandAttackProfile(
        boolean offHand,
        ItemStack weapon,
        double weaponBaseAttack,
        int aspdRo,
        double aps,
        double physicalAttack,
        double accuracy,
        double critChance,
        double critDamageMultiplier) {
}
