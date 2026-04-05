package com.etema.ragnarmmo.roitems.runtime;

import com.etema.ragnarmmo.roitems.ZenyItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ZenyWalletHelper {
    public static final int COPPER_VALUE = 1;
    public static final int SILVER_VALUE = 9;
    public static final int GOLD_VALUE = 81;

    private ZenyWalletHelper() {
    }

    public static boolean isZeny(ItemStack stack) {
        return stack.is(ZenyItems.COPPER_ZENY.get())
                || stack.is(ZenyItems.SILVER_ZENY.get())
                || stack.is(ZenyItems.GOLD_ZENY.get());
    }

    public static int getTotalZeny(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            total += stack.getCount() * getValue(stack);
        }
        return total;
    }

    public static boolean tryConsume(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        int total = getTotalZeny(player);
        if (total < amount) {
            return false;
        }

        player.getInventory().clearOrCountMatchingItems(ZenyWalletHelper::isZeny, Integer.MAX_VALUE,
                player.inventoryMenu.getCraftSlots());
        addZeny(player, total - amount);
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static void addZeny(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        int gold = amount / GOLD_VALUE;
        amount %= GOLD_VALUE;
        int silver = amount / SILVER_VALUE;
        int copper = amount % SILVER_VALUE;

        addItem(player, ZenyItems.GOLD_ZENY.get(), gold);
        addItem(player, ZenyItems.SILVER_ZENY.get(), silver);
        addItem(player, ZenyItems.COPPER_ZENY.get(), copper);
    }

    public static String formatZeny(int amount) {
        int gold = amount / GOLD_VALUE;
        amount %= GOLD_VALUE;
        int silver = amount / SILVER_VALUE;
        int copper = amount % SILVER_VALUE;

        StringBuilder builder = new StringBuilder();
        if (gold > 0) {
            builder.append(gold).append(" Gold");
        }
        if (silver > 0) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(silver).append(" Silver");
        }
        if (copper > 0 || builder.length() == 0) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(copper).append(" Copper");
        }
        return builder.toString();
    }

    private static int getValue(ItemStack stack) {
        if (stack.is(ZenyItems.GOLD_ZENY.get())) {
            return GOLD_VALUE;
        }
        if (stack.is(ZenyItems.SILVER_ZENY.get())) {
            return SILVER_VALUE;
        }
        return COPPER_VALUE;
    }

    private static void addItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(64, remaining);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= stackSize;
        }
    }
}
