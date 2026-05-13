package com.etema.ragnarmmo.mobs.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Deprecated compatibility packet kept registered so packet IDs after the mob
 * module do not shift during the Jade tooltip migration.
 */
@Deprecated(forRemoval = false)
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
        ctx.setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }
}
