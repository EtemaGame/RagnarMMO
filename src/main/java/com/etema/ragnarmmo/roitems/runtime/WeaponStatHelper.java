package com.etema.ragnarmmo.roitems.runtime;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class WeaponStatHelper {

    private WeaponStatHelper() {
    }

    public static boolean isMagicWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("wands") || path.contains("staves");
        });
    }

    public static double getDisplayedMagicAttack(ItemStack stack) {
        if (!isMagicWeapon(stack)) {
            return 0.0D;
        }

        double base = 1.0D + sumAttribute(stack.getAttributeModifiers(EquipmentSlot.MAINHAND), Attributes.ATTACK_DAMAGE)
                + RoRefineMath.getAttackBonus(stack);

        boolean staff = stack.getTags().anyMatch(tag -> tag.location().getPath().contains("staves"));
        return Math.max(0.0D, staff ? base + 1.0D : base);
    }

    private static double sumAttribute(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
        double total = 0.0D;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            total += modifier.getAmount();
        }
        return total;
    }
}
