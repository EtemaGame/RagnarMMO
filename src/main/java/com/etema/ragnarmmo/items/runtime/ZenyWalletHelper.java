package com.etema.ragnarmmo.items.runtime;

import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.economy.zeny.capability.PlayerWalletProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ZenyWalletHelper {
    public static final int COPPER_VALUE = 1;
    public static final int SILVER_VALUE = 9;
    public static final int GOLD_VALUE = 81; // 9 * 9

    private ZenyWalletHelper() {
    }

    public static boolean isZeny(ItemStack stack) {
        return stack.is(ZenyItems.COPPER_ZENY.get())
                || stack.is(ZenyItems.SILVER_ZENY.get())
                || stack.is(ZenyItems.GOLD_ZENY.get());
    }

    public static int getTotalZeny(ServerPlayer player) {
        // Now exclusively delegated to capability
        return PlayerWalletProvider.get(player)
                .map(wallet -> (int)Math.min(Integer.MAX_VALUE, wallet.getZeny()))
                .orElse(0);
    }

    public static boolean tryConsume(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        return PlayerWalletProvider.get(player)
                .map(wallet -> {
                    boolean success = wallet.consumeZeny(amount);
                    if (success) {
                        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                                new com.etema.ragnarmmo.economy.zeny.network.WalletSyncPacket(wallet.getZeny()));
                    }
                    return success;
                })
                .orElse(false);
    }

    public static void addZeny(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerWalletProvider.get(player).ifPresent(wallet -> {
            wallet.addZeny(amount);
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                    new com.etema.ragnarmmo.economy.zeny.network.WalletSyncPacket(wallet.getZeny()));
        });
    }

    public static String formatZeny(long amount) {
        return amount + " Zeny";
    }

    public static int getValue(ItemStack stack) {
        if (stack.is(ZenyItems.GOLD_ZENY.get())) {
            return GOLD_VALUE;
        }
        if (stack.is(ZenyItems.SILVER_ZENY.get())) {
            return SILVER_VALUE;
        }
        if (stack.is(ZenyItems.COPPER_ZENY.get())) {
            return COPPER_VALUE;
        }
        return 0;
    }
}
