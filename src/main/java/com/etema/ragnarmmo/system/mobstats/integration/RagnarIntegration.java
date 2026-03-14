package com.etema.ragnarmmo.system.mobstats.integration;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class RagnarIntegration {

    private RagnarIntegration() {
    }

    public static boolean isAvailable() {
        return RagnarBridge.hasStatsSupport();
    }

    public static Optional<Integer> getPlayerRPGLevel(Player player) {
        if (!RagnarBridge.hasStatsSupport() || player == null) {
            return Optional.empty();
        }

        return RagnarCoreAPI.get(player).map(IPlayerStats::getLevel);
    }

    public static Optional<DifficultySnapshot> getDifficultySnapshot(ServerLevel level) {
        if (!RagnarBridge.hasStatsSupport() || level == null) {
            return Optional.empty();
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return Optional.empty();
        }

        int total = 0;
        int count = 0;
        int max = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Optional<Integer> levelOpt = getPlayerRPGLevel(player);
            if (levelOpt.isEmpty()) continue;

            int value = levelOpt.get();
            total += value;
            max = Math.max(max, value);
            count++;
        }

        if (count == 0) {
            return Optional.empty();
        }

        int average = (int) Math.round((double) total / (double) count);
        return Optional.of(new DifficultySnapshot(count, average, max));
    }

    public record DifficultySnapshot(int playerCount, int averageLevel, int maxLevel) {
    }
}






