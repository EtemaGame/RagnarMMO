package com.etema.ragnarmmo.system.stats.util;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class AntiFarmManager {

    private static final String KEY_ZONE_X = "ragnarmmo_anti_farm_chunk_x";
    private static final String KEY_ZONE_Z = "ragnarmmo_anti_farm_chunk_z";
    private static final String KEY_ZONE_START_TIME = "ragnarmmo_anti_farm_start_time";
    
    /**
     * Calculates the penalty factor (0.1 to 1.0) based on time spent in the current chunk zone.
     * 1.0 = Full efficiency, 0.1 = 90% reduction.
     */
    public static double getPenaltyFactor(Player player) {
        if (player.level().isClientSide()) return 1.0;
        
        updateZone(player);
        
        long startTime = player.getPersistentData().getLong(KEY_ZONE_START_TIME);
        if (startTime == 0) return 1.0;
        
        long now = System.currentTimeMillis();
        long elapsedMillis = now - startTime;
        long elapsedMinutes = elapsedMillis / 60000;
        
        int threshold = RagnarConfigs.SERVER.progression.antiFarmTimeThreshold.get();
        if (elapsedMinutes < threshold) return 1.0;
        
        // Progressive penalty after threshold
        // Penalty starts at 15 minutes. 
        // At 15 mins: 1.0
        // At 30 mins: 0.5 (example)
        // We'll make it drop by 10% every 5 minutes after the threshold.
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
            // If moved more than 'radius' chunks away, reset
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
