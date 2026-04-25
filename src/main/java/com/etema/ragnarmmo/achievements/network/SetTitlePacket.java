package com.etema.ragnarmmo.achievements.network;

import com.etema.ragnarmmo.achievements.capability.PlayerAchievementsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetTitlePacket {

    private final String title;

    public SetTitlePacket(String title) {
        this.title = title == null ? "" : title;
    }

    public static void encode(SetTitlePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title);
    }

    public static SetTitlePacket decode(FriendlyByteBuf buf) {
        return new SetTitlePacket(buf.readUtf());
    }

    public static void handle(SetTitlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {

                    if (msg.title.isEmpty()) {
                        cap.setActiveTitle(null);
                        syncTitle(player, cap);
                    } else {
                        // Ensure the player actually unlocked an achievement granting this exact title
                        boolean hasUnlockedTitle = cap.getUnlockedAchievements().stream().anyMatch(unlockedId -> {
                            var def = com.etema.ragnarmmo.achievements.data.AchievementRegistry.getInstance()
                                    .get(unlockedId);
                            return def != null && msg.title.equals(def.title());
                        });

                        if (hasUnlockedTitle) {
                            cap.setActiveTitle(msg.title);
                            syncTitle(player, cap);
                        }
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void syncTitle(ServerPlayer player,
            com.etema.ragnarmmo.achievements.capability.IPlayerAchievements cap) {
        // Broadcast the new title to all tracking entities to update their rendered
        // nameplate
        com.etema.ragnarmmo.common.net.Network.sendTrackingEntityAndSelf(player,
                new SyncAchievementsPacket(player.getId(), (net.minecraft.nbt.CompoundTag) cap.serializeNBT()));
        player.refreshDisplayName();
    }
}
