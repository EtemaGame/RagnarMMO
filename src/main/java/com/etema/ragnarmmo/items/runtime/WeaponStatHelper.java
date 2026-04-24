package com.etema.ragnarmmo.items.runtime;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public final class WeaponStatHelper {

    private WeaponStatHelper() {
    }

    public static boolean isMagicWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        double configuredMagic = getConfiguredMagicAttackBase(stack);
        if (configuredMagic > 0.0D) {
            return true;
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("wands") || path.contains("staves");
        });
    }

    public static double getDisplayedMagicAttack(ItemStack stack) {
        double configuredMagic = getConfiguredMagicAttackBase(stack);
        if (configuredMagic > 0.0D) {
            return configuredMagic + RoRefineMath.getAttackBonus(stack);
        }

        if (!isMagicWeapon(stack)) {
            return 0.0D;
        }

        double base = 1.0D + sumAttribute(stack.getAttributeModifiers(EquipmentSlot.MAINHAND), Attributes.ATTACK_DAMAGE)
                + RoRefineMath.getAttackBonus(stack);

        boolean staff = stack.getTags().anyMatch(tag -> tag.location().getPath().contains("staves"));
        return Math.max(0.0D, staff ? base + 1.0D : base);
    }

    public static double getConfiguredPhysicalAttackBase(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }

        var profile = RoItemRuleResolver.resolve(stack).combatProfile();
        if (profile.atk() > 0.0D) {
            return profile.atk();
        }
        return sumConfiguredAttributeIds(stack, profile.atkAttributeIds());
    }

    public static double getConfiguredMagicAttackBase(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }

        var profile = RoItemRuleResolver.resolve(stack).combatProfile();
        if (profile.matk() > 0.0D) {
            return profile.matk();
        }
        return sumConfiguredAttributeIds(stack, profile.matkAttributeIds());
    }

    public static int getConfiguredAspd(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var profile = RoItemRuleResolver.resolve(stack).combatProfile();
        if (profile.aspd() > 0) {
            return profile.aspd();
        }

        double customAspd = sumConfiguredAttributeIds(stack, profile.aspdAttributeIds());
        return customAspd > 0.0D ? (int) Math.round(customAspd) : 0;
    }

    public static double getConfiguredRange(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }

        var profile = RoItemRuleResolver.resolve(stack).combatProfile();
        if (profile.range() > 0.0D) {
            return profile.range();
        }
        return sumConfiguredAttributeIds(stack, profile.rangeAttributeIds());
    }

    private static double sumConfiguredAttributeIds(ItemStack stack, Set<String> attributeIds) {
        if (stack == null || stack.isEmpty() || attributeIds == null || attributeIds.isEmpty()) {
            return 0.0D;
        }

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        double total = 0.0D;
        for (var entry : modifiers.entries()) {
            ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attributeId == null || !attributeIds.contains(attributeId.toString())) {
                continue;
            }
            total += entry.getValue().getAmount();
        }
        return total;
    }

    private static double sumAttribute(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
        double total = 0.0D;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            total += modifier.getAmount();
        }
        return total;
    }
}
