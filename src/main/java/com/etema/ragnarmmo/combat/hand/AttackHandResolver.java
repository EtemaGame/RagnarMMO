package com.etema.ragnarmmo.combat.hand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;

/**
 * Central equipment rules for deciding which hand can author a basic attack.
 */
public final class AttackHandResolver {

    private AttackHandResolver() {
    }

    public static boolean isDualWielding(Player player) {
        if (player == null) {
            return false;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return isAttackWeapon(mainHand) && isAttackWeapon(offHand) && !isShield(offHand);
    }

    public static boolean isValidAttackHand(Player player, boolean offHand) {
        if (player == null) {
            return false;
        }
        return !offHand || isDualWielding(player);
    }

    public static boolean shouldResetCycle(Player player) {
        return !isDualWielding(player);
    }

    public static boolean resolveNextHand(Player player, boolean previousOffHand) {
        return isDualWielding(player) && !previousOffHand;
    }

    private static boolean isAttackWeapon(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !isShield(stack)
                && stack.getItem() instanceof TieredItem;
    }

    private static boolean isShield(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                        || stack.getItem() instanceof ShieldItem);
    }
}
