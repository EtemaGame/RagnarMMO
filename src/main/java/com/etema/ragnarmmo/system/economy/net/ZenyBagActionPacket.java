package com.etema.ragnarmmo.system.economy.net;

import com.etema.ragnarmmo.roitems.ZenyItems;
import com.etema.ragnarmmo.roitems.runtime.ZenyWalletHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ZenyBagActionPacket {

    public enum Action {
        DEPOSIT_ALL,
        WITHDRAW_GOLD,
        WITHDRAW_SILVER,
        WITHDRAW_COPPER
    }

    private final Action action;

    public ZenyBagActionPacket(Action action) {
        this.action = action;
    }

    public static void encode(ZenyBagActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static ZenyBagActionPacket decode(FriendlyByteBuf buf) {
        return new ZenyBagActionPacket(buf.readEnum(Action.class));
    }

    public static void handle(ZenyBagActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (msg.action) {
                case DEPOSIT_ALL:
                    handleDepositAll(player);
                    break;
                case WITHDRAW_GOLD:
                    handleWithdraw(player, ZenyWalletHelper.GOLD_VALUE, ZenyItems.GOLD_ZENY.get());
                    break;
                case WITHDRAW_SILVER:
                    handleWithdraw(player, ZenyWalletHelper.SILVER_VALUE, ZenyItems.SILVER_ZENY.get());
                    break;
                case WITHDRAW_COPPER:
                    handleWithdraw(player, ZenyWalletHelper.COPPER_VALUE, ZenyItems.COPPER_ZENY.get());
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleDepositAll(ServerPlayer player) {
        int totalToDeposit = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ZenyWalletHelper.isZeny(stack)) {
                totalToDeposit += ZenyWalletHelper.getValue(stack) * stack.getCount();
                stack.setCount(0);
            }
        }

        if (totalToDeposit > 0) {
            ZenyWalletHelper.addZeny(player, totalToDeposit);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    private static void handleWithdraw(ServerPlayer player, int value, net.minecraft.world.item.Item coinItem) {
        if (ZenyWalletHelper.tryConsume(player, value)) {
            ItemStack stack = new ItemStack(coinItem);
            if (!player.getInventory().add(stack)) {
                ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                player.level().addFreshEntity(itemEntity);
            }
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }
}
