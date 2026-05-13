package com.etema.ragnarmmo.bestiary.network;

import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDetailsDto;
import com.etema.ragnarmmo.bestiary.data.BestiaryClientRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncBestiaryDetailsPacket(BestiaryEntryDetailsDto details) {
    public static void encode(SyncBestiaryDetailsPacket msg, FriendlyByteBuf buf) {
        BestiaryEntryDetailsDto.encode(msg.details, buf);
    }

    public static SyncBestiaryDetailsPacket decode(FriendlyByteBuf buf) {
        return new SyncBestiaryDetailsPacket(BestiaryEntryDetailsDto.decode(buf));
    }

    public static void handle(SyncBestiaryDetailsPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BestiaryClientRegistry.putDetails(msg.details)));
        ctx.setPacketHandled(true);
    }
}
