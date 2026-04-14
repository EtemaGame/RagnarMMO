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
            if (player == null || player.getServer() == null) {
                return;
            }

            // Rule 1 & 2: Server checks flags and permissions authoritatively
            if (!MobStatsConfigAccess.isManualMobEditorEnabled() || !player.hasPermissions(2)) {
                return;
            }

            try {
                // Rule 4 & Save Contract: Validate and Sanitize
                com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntryValidator.validateOrThrow(msg.entry);

                InternalManualMobEntry sanitized = new InternalManualMobEntry(
                        msg.entry.entityTypeId(),
                        msg.entry.enabled(),
                        msg.entry.level(),
                        msg.entry.rank(),
                        msg.entry.race(),
                        msg.entry.element(),
                        msg.entry.size(),
                        msg.entry.maxHp(),
                        msg.entry.atkMin(),
                        msg.entry.atkMax(),
                        msg.entry.def(),
                        msg.entry.mdef(),
                        msg.entry.hit(),
                        msg.entry.flee(),
                        msg.entry.crit(),
                        msg.entry.aspd(),
                        msg.entry.moveSpeed(),
                        msg.entry.notes(),
                        player.getScoreboardName(), // Authoritative identity
                        System.currentTimeMillis()   // Authoritative time
                );

                ManualMobRegistryService.upsert(player.getServer(), sanitized);

                // Rule 5: Send back CANONICAL state
                var detail = ManualMobRegistryService.buildDetail(player.getServer(), sanitized.entityTypeId(), player);
                Network.sendToPlayer(player, new ManualMobDetailResponsePacket(detail));

            } catch (Exception e) {
                // In a production environment, we should send an error packet back to the client
                e.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
