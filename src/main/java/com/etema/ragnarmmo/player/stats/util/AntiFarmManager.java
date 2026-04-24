package com.etema.ragnarmmo.player.stats.util;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public final class AntiFarmManager {

    private static final String KEY_ZONE_X = "ragnarmmo_anti_farm_chunk_x";
    private static final String KEY_ZONE_Z = "ragnarmmo_anti_farm_chunk_z";
    private static final String KEY_ZONE_START_TIME = "ragnarmmo_anti_farm_start_time";

    private AntiFarmManager() {
    }

    public static double getPenaltyFactor(Player player) {
        if (player.level().isClientSide()) {
            return 1.0;
        }

        updateZone(player);

        long startTime = player.getPersistentData().getLong(KEY_ZONE_START_TIME);
        if (startTime == 0) {
            return 1.0;
        }

        long now = System.currentTimeMillis();
        long elapsedMillis = now - startTime;
        long elapsedMinutes = elapsedMillis / 60000;

        int threshold = RagnarConfigs.SERVER.progression.antiFarmTimeThreshold.get();
        if (elapsedMinutes < threshold) {
            return 1.0;
        }

        double excessMinutes = elapsedMinutes - threshold;
        double reduction = (excessMinutes / 5.0) * 0.1;

        double minEfficiency = RagnarConfigs.SERVER.progression.antiFarmMaxPenalty.get();
        return Math.max(minEfficiency, 1.0 - reduction);
    }

    public static void updateZone(Player player) {
        ChunkPos current = player.chunkPosition();

        if (player.getPersistentData().contains(KEY_ZONE_X)) {
            int lastX = player.getPersistentData().getInt(KEY_ZONE_X);
            int lastZ = player.getPersistentData().getInt(KEY_ZONE_Z);
            int dx = Math.abs(current.x - lastX);
            int dz = Math.abs(current.z - lastZ);

            int radius = RagnarConfigs.SERVER.progression.antiFarmRadiusChunks.get();
            if (dx > radius || dz > radius) {
                resetZone(player, current);
            }
        } else {
            resetZone(player, current);
        }
    }

    private static void resetZone(Player player, ChunkPos chunk) {
        player.getPersistentData().putInt(KEY_ZONE_X, chunk.x);
        player.getPersistentData().putInt(KEY_ZONE_Z, chunk.z);
        player.getPersistentData().putLong(KEY_ZONE_START_TIME, System.currentTimeMillis());
    }
}
