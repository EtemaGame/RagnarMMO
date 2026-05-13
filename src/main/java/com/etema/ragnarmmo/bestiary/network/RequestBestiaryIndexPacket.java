package com.etema.ragnarmmo.bestiary.network;

import com.etema.ragnarmmo.bestiary.data.BestiaryRegistry;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestBestiaryIndexPacket() {
    public static void encode(RequestBestiaryIndexPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestBestiaryIndexPacket decode(FriendlyByteBuf buf) {
        return new RequestBestiaryIndexPacket();
    }

    public static void handle(RequestBestiaryIndexPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            var snapshot = BestiaryRegistry.getInstance().currentIndex();
            Network.sendToPlayer(player, new SyncBestiaryIndexPacket(snapshot.version(), snapshot.entries()));
        });
        ctx.setPacketHandled(true);
    }
}
