package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobDeleteEntryPacket {
    private final ResourceLocation entityTypeId;

    public ManualMobDeleteEntryPacket(ResourceLocation entityTypeId) { this.entityTypeId = entityTypeId; }

    public static void encode(ManualMobDeleteEntryPacket msg, FriendlyByteBuf buf) { buf.writeResourceLocation(msg.entityTypeId); }

    public static ManualMobDeleteEntryPacket decode(FriendlyByteBuf buf) { return new ManualMobDeleteEntryPacket(buf.readResourceLocation()); }

    public static void handle(ManualMobDeleteEntryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.getServer() == null) {
                return;
            }

            // Rule 1 & 2: Server checks flags and permissions authoritatively
            if (!MobStatsConfigAccess.isManualMobEditorEnabled() || !player.hasPermissions(2)) {
                return;
            }

            // Delete Contract: Authoritatively remove and return fresh state
            com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService.remove(player.getServer(), msg.entityTypeId);

            // Rule 5: Send back CANONICAL state (Detail will now show internalPresent = false)
            var detail = com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService.buildDetail(player.getServer(), msg.entityTypeId, player);
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, new ManualMobDetailResponsePacket(detail));
        });
        ctx.get().setPacketHandled(true);
    }
}
