package com.etema.ragnarmmo.system.mobstats.util;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class MobUtils {

    /**
     * Identifies if a mob is a Boss/MVP.
     */
    public static boolean isMVPBoss(LivingEntity mob) {
        if (MobSpawnOverrides.isManualBoss(mob)) {
            return true;
        }

        // Check MobStats capability for tier
        var mobStatsOpt = MobStatsProvider.get(mob);
        if (mobStatsOpt.isPresent()) {
            var stats = mobStatsOpt.resolve().orElse(null);
            if (stats != null) {
                var tier = stats.getTier();
                if (tier.isBossLike()) {
                    return true;
                }
            }
        }

        // Fallback: check vanilla boss types
        return mob instanceof WitherBoss || mob instanceof EnderDragon;
    }
}
