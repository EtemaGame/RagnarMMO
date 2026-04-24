package com.etema.ragnarmmo.player.stats.progression;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import net.minecraft.world.entity.player.Player;

public final class ThresholdBonusService {

    private ThresholdBonusService() {
    }

    public static void recomputeThresholds(Player player, IPlayerStats stats) {
        if (player == null || stats == null) {
            return;
        }
    }
}
