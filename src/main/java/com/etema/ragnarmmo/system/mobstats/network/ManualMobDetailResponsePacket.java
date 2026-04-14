package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.client.ClientPacketHandler;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobDetailResponsePacket {
    private final InternalManualMobEntry entry;

    public ManualMobDetailResponsePacket(InternalManualMobEntry entry) { this.entry = entry; }

    public static void encode(ManualMobDetailResponsePacket msg, FriendlyByteBuf buf) { ManualMobPacketsCodec.writeInternalEntry(buf, msg.entry); }

    public static ManualMobDetailResponsePacket decode(FriendlyByteBuf buf) { return new ManualMobDetailResponsePacket(ManualMobPacketsCodec.readInternalEntry(buf)); }

    public static void handle(ManualMobDetailResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleManualMobDetail(msg.entry)));
        ctx.get().setPacketHandled(true);
    }
}
