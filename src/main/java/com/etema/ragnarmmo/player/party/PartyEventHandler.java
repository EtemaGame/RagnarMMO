package com.etema.ragnarmmo.player.party;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles party-related events like player login/logout and periodic cleanup.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class PartyEventHandler {

    // Cleanup interval (5 minutes)
    private static final int CLEANUP_INTERVAL_TICKS = 20 * 60 * 5;
    private static final int MEMBER_SYNC_INTERVAL_TICKS = 10;
    private static int cleanupCounter = 0;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PartyService service = PartyService.get(player.getServer());
            service.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PartyService service = PartyService.get(player.getServer());
            service.onPlayerLogout(player);
            PartyMemberSyncService.clear(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % MEMBER_SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        PartyMemberSyncService.syncCurrentIfChanged(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL_TICKS) {
            cleanupCounter = 0;

            // Run cleanup on the server
            if (event.getServer() != null) {
                PartyService service = PartyService.get(event.getServer());
                service.cleanup();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Re-sync party data after respawn
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PartyService service = PartyService.get(player.getServer());
            Party party = service.getParty(player);
            if (party != null) {
                service.syncPartyToMembers(party);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Re-sync party data after dimension change
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PartyService service = PartyService.get(player.getServer());
            Party party = service.getParty(player);
            if (party != null) {
                service.syncPartyToMembers(party);
            }
        }
    }
}
