package com.etema.ragnarmmo.economy.zeny.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class WalletSyncPacket {
    public final long zeny;

    public WalletSyncPacket(long zeny) {
        this.zeny = zeny;
    }

    public static void encode(WalletSyncPacket m, FriendlyByteBuf buf) {
        buf.writeLong(m.zeny);
    }

    public static WalletSyncPacket decode(FriendlyByteBuf buf) {
        return new WalletSyncPacket(buf.readLong());
    }

    public static void handle(WalletSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleWalletSync(msg)));
        ctx.setPacketHandled(true);
    }
}
