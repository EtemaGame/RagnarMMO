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
            if (player == null || !MobStatsConfigAccess.isManualMobEditorEnabled() || !player.hasPermissions(2)) {
                return;
            }
            com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService.remove(player.getServer(), msg.entityTypeId);
        });
        ctx.get().setPacketHandled(true);
    }
}
