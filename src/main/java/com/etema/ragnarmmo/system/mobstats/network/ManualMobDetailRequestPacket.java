package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobDetailRequestPacket {
    private final ResourceLocation entityTypeId;

    public ManualMobDetailRequestPacket(ResourceLocation entityTypeId) { this.entityTypeId = entityTypeId; }

    public static void encode(ManualMobDetailRequestPacket msg, FriendlyByteBuf buf) { buf.writeResourceLocation(msg.entityTypeId); }

    public static ManualMobDetailRequestPacket decode(FriendlyByteBuf buf) { return new ManualMobDetailRequestPacket(buf.readResourceLocation()); }

    public static void handle(ManualMobDetailRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            // Rule 3: No faked defaults. buildDetail handles null internalEntry correctly.
            var detail = ManualMobRegistryService.buildDetail(player.getServer(), msg.entityTypeId, player);
            Network.sendToPlayer(player, new ManualMobDetailResponsePacket(detail));
        });
        ctx.get().setPacketHandled(true);
    }
}
