package com.etema.ragnarmmo.combat.resolver;

import com.etema.ragnarmmo.items.runtime.RangedWeaponStatsHelper;
import com.etema.ragnarmmo.items.runtime.WeaponStatHelper;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

public final class WeaponClassificationService {
    private WeaponClassificationService() {
    }

    public static boolean isRangedWeapon(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return false;
        }
        Item item = weapon.getItem();
        return item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || RangedWeaponStatsHelper.hasManualProfile(weapon);
    }

    public static int baseAspd(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return 180;
        }

        int configuredAspd = WeaponStatHelper.getConfiguredAspd(weapon);
        if (configuredAspd > 0) {
            return configuredAspd;
        }

        Item item = weapon.getItem();
        boolean isDagger = hasTagPath(weapon, "daggers");
        boolean isMace = hasTagPath(weapon, "maces");
        boolean isStaff = hasTagPath(weapon, "staves");
        boolean isWand = hasTagPath(weapon, "wands");
        boolean isTwoHanded = hasTagPath(weapon, "two_handed");

        if (isDagger) return 178;
        if (isWand) return 172;
        if (isStaff) return 165;
        if (isMace) return 160;
        if (item instanceof SwordItem) return isTwoHanded ? 158 : 170;
        if (item instanceof AxeItem) return isTwoHanded ? 150 : 155;
        if (item instanceof ShieldItem) return 150;
        if (item instanceof BowItem || item instanceof CrossbowItem) return 170;
        if (item instanceof PickaxeItem) return 160;
        if (item instanceof ShovelItem) return 165;
        if (item instanceof HoeItem) return 175;
        if (item instanceof TridentItem) return 150;

        return 170;
    }

    private static boolean hasTagPath(ItemStack weapon, String pathPart) {
        return weapon.getTags().anyMatch(tag -> tag.location().getPath().contains(pathPart));
    }
}
