package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.stats.compute.EquipmentStatSnapshot;
import com.etema.ragnarmmo.system.stats.compute.StatComputer;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerStatsSyncService {

    private PlayerStatsSyncService() {
    }

    public static void sync(ServerPlayer player, IPlayerStats stats) {
        sync(player, stats, RoPlayerSyncDomain.allMask());
    }

    public static void sync(ServerPlayer player, IPlayerStats stats, int syncMask) {
        if (player == null || stats == null || syncMask == 0) {
            return;
        }

        RagnarDebugLog.playerData(
                "SYNC source=service player={} mask={} derived={} baseLv={} jobLv={} exp={} jobExp={} hpRes={}/{} sp={}/{}",
                player.getGameProfile().getName(),
                RoPlayerSyncDomain.describeMask(syncMask),
                RoPlayerSyncDomain.requiresDerivedSync(syncMask),
                stats.getLevel(),
                stats.getJobLevel(),
                stats.getExp(),
                stats.getJobExp(),
                RagnarDebugLog.formatDouble(stats.getMana()),
                RagnarDebugLog.formatDouble(stats.getManaMax()),
                RagnarDebugLog.formatDouble(stats.getSP()),
                RagnarDebugLog.formatDouble(stats.getSPMax()));

        Network.sendToPlayer(player, new PlayerStatsSyncPacket(stats, syncMask));

        if (RoPlayerSyncDomain.requiresDerivedSync(syncMask)) {
            var derived = StatComputer.compute(player, stats, EquipmentStatSnapshot.capture(player));
            Network.sendToPlayer(player, new DerivedStatsSyncPacket(derived));
        }
    }
}
