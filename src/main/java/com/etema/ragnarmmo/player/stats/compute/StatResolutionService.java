package com.etema.ragnarmmo.player.stats.compute;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.player.stats.progression.JobBonusService;
import com.etema.ragnarmmo.player.stats.progression.ThresholdBonusService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * StatResolutionService - The central pipeline for resolving player statistics.
 * This service ensures all bonus layers (Job, Threshold, etc.) are applied in the 
 * correct order and then triggers derived stat computation.
 */
public final class StatResolutionService {

    private StatResolutionService() {
    }

    /**
     * Fully recomputes and applies all bonus layers to the player.
     * Should be called on login, level up, job change, or stat point spending.
     */
    public static void resolve(Player player, IPlayerStats stats) {
        if (player == null || stats == null) return;

        // 1. Apply Job Bonuses (Modifiers)
        JobBonusService.recomputeStats(player, stats);

        // 2. Apply Threshold Bonuses (Modifiers - every 10 points)
        ThresholdBonusService.recomputeThresholds(player, stats);

        // 3. Compute Derived Stats (Server side truth)
        if (player instanceof ServerPlayer sp) {
            var derived = StatComputer.compute(sp, stats, EquipmentStatSnapshot.capture(sp));

            // 4. Sync to Client (The HUD must reflect this truth)
            com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService.sync(sp, stats);
        }
    }

    /**
     * Internal version that just computes the statistics without triggering
     * a sync or marking the capability as dirty. Used by the combat engine
     * for high-frequency calculations.
     */
    public static com.etema.ragnarmmo.common.api.compute.DerivedStats computeAuthoritative(Player player, IPlayerStats stats) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return null;
        }
        return StatComputer.compute(sp, stats, EquipmentStatSnapshot.capture(sp));
    }
}
