package com.etema.ragnarmmo.bestiary.network;

import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDto;
import com.etema.ragnarmmo.bestiary.data.BestiaryClientRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncBestiaryIndexPacket(int version, List<BestiaryEntryDto> entries) {
    public static final int MAX_ENTRIES = 4096;

    public SyncBestiaryIndexPacket {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("bestiary index exceeds " + MAX_ENTRIES + " entries");
        }
    }

    public static void encode(SyncBestiaryIndexPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.version);
        buf.writeVarInt(msg.entries.size());
        for (BestiaryEntryDto entry : msg.entries) {
            BestiaryEntryDto.encode(entry, buf);
        }
    }

    public static SyncBestiaryIndexPacket decode(FriendlyByteBuf buf) {
        int version = buf.readVarInt();
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("invalid bestiary index size: " + size);
        }
        List<BestiaryEntryDto> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(BestiaryEntryDto.decode(buf));
        }
        return new SyncBestiaryIndexPacket(version, entries);
    }

    public static void handle(SyncBestiaryIndexPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BestiaryClientRegistry.replace(msg.version, msg.entries)));
        ctx.setPacketHandled(true);
    }
}
