package com.etema.ragnarmmo.achievements.network;

import com.etema.ragnarmmo.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.achievements.data.AchievementRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ClaimAchievementPacket {

    private final String achievementId;

    public ClaimAchievementPacket(String achievementId) {
        this.achievementId = achievementId;
    }

    public static void encode(ClaimAchievementPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.achievementId);
    }

    public static ClaimAchievementPacket decode(FriendlyByteBuf buf) {
        return new ClaimAchievementPacket(buf.readUtf());
    }

    public static void handle(ClaimAchievementPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
                    // Must be unlocked but not yet claimed
                    if (cap.isUnlocked(msg.achievementId) && !cap.isClaimed(msg.achievementId)) {
                        AchievementDefinition def = AchievementRegistry.getInstance().get(msg.achievementId);
                        if (def != null && def.rewards() != null) {

                            // Try generating items
                            def.rewards().forEach((itemRegId, count) -> {
                                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemRegId));
                                if (item != null) {
                                    ItemStack stack = new ItemStack(item, count);
                                    if (!player.getInventory().add(stack)) {
                                        player.drop(stack, false);
                                    }
                                }
                            });

                            // Mark claimed
                            cap.claimReward(msg.achievementId);

                            // Sync state
                            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                                    new SyncAchievementsPacket(player.getId(), (net.minecraft.nbt.CompoundTag) cap.serializeNBT()));
                        }
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
