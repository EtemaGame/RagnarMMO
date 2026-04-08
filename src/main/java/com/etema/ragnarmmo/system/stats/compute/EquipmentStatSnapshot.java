package com.etema.ragnarmmo.system.stats.compute;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.roitems.runtime.RagnarRangedWeaponStats;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.roitems.runtime.RoRefineMath;
import com.etema.ragnarmmo.roitems.runtime.WeaponStatHelper;
import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Immutable server-authoritative equipment snapshot used by the stat pipeline.
 * All callers should resolve combat-relevant equipment state through this object
 * instead of passing loosely related doubles around.
 */
public record EquipmentStatSnapshot(
        double weaponAtk,
        double weaponMagicAtk,
        int weaponBaseAspd,
        double armorHardDef,
        double armorHardMdef,
        boolean hasShield,
        boolean rangedWeapon,
        double baseCastTime
) {
    public static final double DEFAULT_BASE_CAST_TIME = 1.0D;

    public static EquipmentStatSnapshot capture(Player player) {
        return capture(player, DEFAULT_BASE_CAST_TIME);
    }

    public static EquipmentStatSnapshot capture(Player player, double baseCastTime) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean hasShield = player.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                || player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
        boolean rangedWeapon = CombatMath.isRangedWeapon(main);

        double weaponAtk;
        int weaponBaseAspd = CombatMath.getWeaponBaseASPD(main);
        if (main.getItem() instanceof RagnarRangedWeaponStats rangedStats) {
            weaponAtk = rangedStats.getRangedWeaponAtk(main) + RoRefineMath.getAttackBonus(main);
            weaponBaseAspd = rangedStats.getBaseRangedAspd(main);
        } else {
            weaponAtk = resolveMeleeWeaponAttack(main, player);
        }

        return new EquipmentStatSnapshot(
                weaponAtk,
                WeaponStatHelper.getDisplayedMagicAttack(main),
                weaponBaseAspd,
                computeArmorHardDef(player),
                computeArmorHardMdef(player),
                hasShield,
                rangedWeapon,
                baseCastTime);
    }

    public static double computeArmorHardDef(LivingEntity entity) {
        double armorEff = entity.getArmorValue();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                armorEff += RoRefineMath.getDefenseBonus(stack);
            }
        }

        return armorEff;
    }

    public static double computeArmorHardMdef(LivingEntity entity) {
        double equipMdef = 0.0D;
        var attrInstance = entity.getAttribute(RagnarAttributes.MAGIC_DEFENSE.get());
        if (attrInstance != null) {
            equipMdef += attrInstance.getValue();
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int refine = RoItemNbtHelper.getRefineLevel(stack);
            if (refine >= 5) {
                equipMdef += (refine - 4);
            }
        }

        return equipMdef;
    }

    private static double resolveMeleeWeaponAttack(ItemStack main, Player player) {
        double base = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        Multimap<Attribute, AttributeModifier> mods = main.getAttributeModifiers(EquipmentSlot.MAINHAND);

        double add = 0.0D;
        double multBase = 0.0D;
        double multTotal = 0.0D;
        for (var entry : mods.entries()) {
            if (entry.getKey() != Attributes.ATTACK_DAMAGE) {
                continue;
            }
            AttributeModifier modifier = entry.getValue();
            switch (modifier.getOperation()) {
                case ADDITION -> add += modifier.getAmount();
                case MULTIPLY_BASE -> multBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multTotal += modifier.getAmount();
            }
        }

        double withItem = (base * (1.0D + multBase) + add) * (1.0D + multTotal);
        float enchantDamage = EnchantmentHelper.getDamageBonus(main, MobType.UNDEFINED);
        return withItem + enchantDamage + RoRefineMath.getAttackBonus(main);
    }
}
