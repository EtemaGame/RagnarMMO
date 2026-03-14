package com.etema.ragnarmmo.system.mobstats.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server-to-client packet that notifies when a mob has been hurt.
 * This enables the client to show health bars for entities that were
 * damaged on the server side (dedicated server compatibility).
 */
public class MobHurtPacket {
    private final int entityId;

    public MobHurtPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(MobHurtPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static MobHurtPacket decode(FriendlyByteBuf buf) {
        return new MobHurtPacket(buf.readVarInt());
    }

    public static void handle(MobHurtPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleMobHurt(msg.entityId)));
        ctx.setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }
}
