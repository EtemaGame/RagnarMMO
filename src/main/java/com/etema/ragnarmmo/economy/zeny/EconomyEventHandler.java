package com.etema.ragnarmmo.economy.zeny;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.economy.zeny.capability.PlayerWalletProvider;
import com.etema.ragnarmmo.economy.zeny.network.WalletSyncPacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class EconomyEventHandler {

    public static final ResourceLocation WALLET_CAP_ID = ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, "player_wallet");

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerWalletProvider.PLAYER_WALLET).isPresent()) {
                event.addCapability(WALLET_CAP_ID, new PlayerWalletProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        original.reviveCaps();
        PlayerWalletProvider.get(original).ifPresent(oldWallet -> {
            PlayerWalletProvider.get(clone).ifPresent(newWallet -> {
                newWallet.deserializeNBT(oldWallet.serializeNBT());
            });
        });
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncWallet(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncWallet(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncWallet(event.getEntity());
    }

    private static void syncWallet(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide) {
            PlayerWalletProvider.get(player).ifPresent(wallet -> {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(serverPlayer,
                        new WalletSyncPacket(wallet.getZeny()));
            });
        }
    }
}
