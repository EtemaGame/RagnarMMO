package com.etema.ragnarmmo.player.stats.compute;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.items.runtime.RangedWeaponStatsHelper;
import com.etema.ragnarmmo.items.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.items.runtime.RoRefineMath;
import com.etema.ragnarmmo.items.runtime.WeaponStatHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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
        if (rangedWeapon) {
            var rangedStats = RangedWeaponStatsHelper.resolve(main);
            if (rangedStats.isPresent()) {
                weaponAtk = rangedStats.get().weaponAtk();
                weaponBaseAspd = rangedStats.get().baseAspd();
            } else {
                weaponAtk = resolveMeleeWeaponAttack(main, player);
            }
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
        double armorEff = 0.0D;

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
        double configuredAttack = WeaponStatHelper.getConfiguredPhysicalAttackBase(main);
        if (configuredAttack > 0.0D) {
            float enchantDamage = EnchantmentHelper.getDamageBonus(main, MobType.UNDEFINED);
            return configuredAttack + enchantDamage + RoRefineMath.getAttackBonus(main);
        }

        // P0 RO combat does not read vanilla attack attributes as balance input.
        // Unauthored weapons get a neutral base until covered by RO item rules.
        float enchantDamage = EnchantmentHelper.getDamageBonus(main, MobType.UNDEFINED);
        return Math.max(1.0D, 1.0D + enchantDamage + RoRefineMath.getAttackBonus(main));
    }
}
