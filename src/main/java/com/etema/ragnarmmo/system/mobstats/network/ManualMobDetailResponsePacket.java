package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.client.ClientPacketHandler;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobDetailResponsePacket {
    private final ManualMobDetail detail;

    public ManualMobDetailResponsePacket(ManualMobDetail detail) { this.detail = detail; }

    public static void encode(ManualMobDetailResponsePacket msg, FriendlyByteBuf buf) { ManualMobPacketsCodec.writeDetail(buf, msg.detail); }

    public static ManualMobDetailResponsePacket decode(FriendlyByteBuf buf) { return new ManualMobDetailResponsePacket(ManualMobPacketsCodec.readDetail(buf)); }

    public static void handle(ManualMobDetailResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleManualMobDetail(msg.detail)));
        ctx.get().setPacketHandled(true);
    }
}
