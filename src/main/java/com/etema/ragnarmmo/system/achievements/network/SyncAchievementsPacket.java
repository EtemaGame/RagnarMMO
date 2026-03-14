package com.etema.ragnarmmo.system.achievements.network;

import com.etema.ragnarmmo.system.achievements.capability.IPlayerAchievements;
import com.etema.ragnarmmo.system.achievements.capability.PlayerAchievementsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncAchievementsPacket {

    private final CompoundTag tag;

    public SyncAchievementsPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(SyncAchievementsPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    public static SyncAchievementsPacket decode(FriendlyByteBuf buf) {
        return new SyncAchievementsPacket(buf.readNbt());
    }

    public static void handle(SyncAchievementsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
                    cap.deserializeNBT(msg.tag);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
