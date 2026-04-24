package com.etema.ragnarmmo.player.party;

import com.etema.ragnarmmo.player.party.net.PartyMemberData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes the server-owned party member projection used by the client HUD.
 */
public final class PartyMemberSyncService {
    private static final Map<UUID, PartyMemberData> LAST_SENT = new ConcurrentHashMap<>();

    private PartyMemberSyncService() {
    }

    public static void syncCurrent(ServerPlayer player) {
        syncCurrent(player, false);
    }

    public static void syncCurrentIfChanged(ServerPlayer player) {
        syncCurrent(player, true);
    }

    private static void syncCurrent(ServerPlayer player, boolean onlyIfChanged) {
        if (player == null || player.getServer() == null) {
            return;
        }

        MinecraftServer server = player.getServer();
        Party party = PartySavedData.get(server).getPartyByPlayer(player.getUUID());
        if (party == null) {
            clear(player.getUUID());
            return;
        }

        PartyMemberData memberData = PartyMemberData.fromPlayer(player, party.isLeader(player.getUUID()));
        if (memberData == null) {
            return;
        }

        if (onlyIfChanged && memberData.equals(LAST_SENT.get(player.getUUID()))) {
            return;
        }

        LAST_SENT.put(player.getUUID(), memberData);
        PartyService.get(server).syncMemberToMembers(party, memberData);
    }

    public static void clear(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        LAST_SENT.remove(playerUuid);
        PartyXpService.clearPlayerThrottle(playerUuid);
    }
}
