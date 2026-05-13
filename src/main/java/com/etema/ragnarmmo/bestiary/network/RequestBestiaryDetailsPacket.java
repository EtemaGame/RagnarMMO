package com.etema.ragnarmmo.bestiary.network;

import com.etema.ragnarmmo.bestiary.data.BestiaryRegistry;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestBestiaryDetailsPacket(ResourceLocation entityId) {
    public static void encode(RequestBestiaryDetailsPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.entityId);
    }

    public static RequestBestiaryDetailsPacket decode(FriendlyByteBuf buf) {
        return new RequestBestiaryDetailsPacket(buf.readResourceLocation());
    }

    public static void handle(RequestBestiaryDetailsPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            BestiaryRegistry.getInstance().details(msg.entityId, player.server)
                    .ifPresent(details -> Network.sendToPlayer(player, new SyncBestiaryDetailsPacket(details)));
        });
        ctx.setPacketHandled(true);
    }
}
