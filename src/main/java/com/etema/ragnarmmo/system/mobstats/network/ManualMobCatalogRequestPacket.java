package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogService;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobCatalogRequestPacket {

    public ManualMobCatalogRequestPacket() {
    }

    public static void encode(ManualMobCatalogRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static ManualMobCatalogRequestPacket decode(FriendlyByteBuf buf) {
        return new ManualMobCatalogRequestPacket();
    }

    public static void handle(ManualMobCatalogRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !MobStatsConfigAccess.isManualMobDiscoveryEnabled()) {
                return;
            }
            var entries = ManualMobCatalogService.scan(player.getServer(), new ManualMobCatalogService.Filters(null, null, null, null));
            Network.sendToPlayer(player, new ManualMobCatalogResponsePacket(entries));
        });
        ctx.get().setPacketHandled(true);
    }
}
