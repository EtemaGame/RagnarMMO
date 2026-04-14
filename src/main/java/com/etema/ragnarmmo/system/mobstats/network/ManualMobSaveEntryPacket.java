package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualMobSaveEntryPacket {
    private final InternalManualMobEntry entry;

    public ManualMobSaveEntryPacket(InternalManualMobEntry entry) { this.entry = entry; }

    public static void encode(ManualMobSaveEntryPacket msg, FriendlyByteBuf buf) { ManualMobPacketsCodec.writeInternalEntry(buf, msg.entry); }

    public static ManualMobSaveEntryPacket decode(FriendlyByteBuf buf) { return new ManualMobSaveEntryPacket(ManualMobPacketsCodec.readInternalEntry(buf)); }

    public static void handle(ManualMobSaveEntryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !MobStatsConfigAccess.isManualMobEditorEnabled() || !player.hasPermissions(2)) {
                return;
            }
            ManualMobRegistryService.upsert(player.getServer(), msg.entry);
            Network.sendToPlayer(player, new ManualMobDetailResponsePacket(msg.entry));
        });
        ctx.get().setPacketHandled(true);
    }
}
