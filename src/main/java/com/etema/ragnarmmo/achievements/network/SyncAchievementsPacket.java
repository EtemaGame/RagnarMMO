package com.etema.ragnarmmo.achievements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncAchievementsPacket {

    private final int entityId;
    private final CompoundTag tag;

    public SyncAchievementsPacket(int entityId, CompoundTag tag) {
        this.entityId = entityId;
        this.tag = tag;
    }

    public static void encode(SyncAchievementsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeNbt(msg.tag);
    }

    public static SyncAchievementsPacket decode(FriendlyByteBuf buf) {
        return new SyncAchievementsPacket(buf.readInt(), buf.readNbt());
    }

    public static void handle(SyncAchievementsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleAchievementsSync(msg.entityId,
                            msg.tag));
        });
        ctx.get().setPacketHandled(true);
    }
}
