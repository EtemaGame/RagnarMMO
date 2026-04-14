package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.client.ClientPacketHandler;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ManualMobCatalogResponsePacket {
    private final List<ManualMobCatalogEntry> entries;

    public ManualMobCatalogResponsePacket(List<ManualMobCatalogEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void encode(ManualMobCatalogResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (ManualMobCatalogEntry entry : msg.entries) {
            ManualMobPacketsCodec.writeCatalogEntry(buf, entry);
        }
    }

    public static ManualMobCatalogResponsePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ManualMobCatalogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(ManualMobPacketsCodec.readCatalogEntry(buf));
        }
        return new ManualMobCatalogResponsePacket(entries);
    }

    public static void handle(ManualMobCatalogResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleManualMobCatalog(msg.entries)));
        ctx.get().setPacketHandled(true);
    }
}
