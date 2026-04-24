package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.system.loot.cards.CardEquipType;

import net.minecraft.world.item.ItemStack;

/**
 * Simple pre-renewal-inspired refine backend.
 *
 * <p>This is intentionally conservative: it gives refine persistent value in
 * combat and tooltips, while the attempt flow itself is handled separately by
 * {@link RoRefineService}.</p>
 */
public final class RoRefineMath {

    private RoRefineMath() {
    }

    public static boolean isRefinable(ItemStack stack) {
        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        return switch (type) {
            case WEAPON, SHIELD, ARMOR, HEADGEAR, SHOES, GARMENT -> true;
            default -> false;
        };
    }

    public static double getAttackBonus(ItemStack stack) {
        if (!isRefinable(stack)) {
            return 0.0;
        }
        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        if (type != CardEquipType.WEAPON) {
            return 0.0;
        }
        int refine = RoItemNbtHelper.getRefineLevel(stack);
        if (refine <= 0) {
            return 0.0;
        }

        double bonus = refine;
        if (refine >= 7) {
            bonus += (refine - 6);
        }
        return bonus;
    }

    public static double getDefenseBonus(ItemStack stack) {
        if (!isRefinable(stack)) {
            return 0.0;
        }
        CardEquipType type = RoEquipmentTypeResolver.resolve(stack);
        if (type == CardEquipType.WEAPON || type == CardEquipType.ACCESSORY || type == CardEquipType.ANY) {
            return 0.0;
        }
        int refine = RoItemNbtHelper.getRefineLevel(stack);
        if (refine <= 0) {
            return 0.0;
        }

        return refine;
    }
}
