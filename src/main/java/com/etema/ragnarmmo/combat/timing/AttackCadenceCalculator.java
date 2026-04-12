package com.etema.ragnarmmo.combat.timing;

import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;

import net.minecraft.world.entity.player.Player;

/**
 * Shared basic-attack cadence calculation for client pacing and server cooldowns.
 */
public final class AttackCadenceCalculator {

    private AttackCadenceCalculator() {
    }

    public static double computeAPS(Player player, boolean offHand) {
        if (player == null) {
            return 1.0D;
        }

        boolean effectiveOffHand = offHand && AttackHandResolver.isValidAttackHand(player, true);
        int agi = (int) StatAttributes.getTotal(player, StatKeys.AGI);
        int dex = (int) StatAttributes.getTotal(player, StatKeys.DEX);
        double aps = CombatMath.computeAPSForAttack(
                player.getMainHandItem(),
                player.getOffhandItem(),
                effectiveOffHand,
                agi,
                dex,
                0.0D);
        return Double.isFinite(aps) && aps > 0.0D ? aps : 1.0D;
    }

    public static int computeIntervalTicks(Player player, boolean offHand) {
        double aps = computeAPS(player, offHand);
        return Math.max(2, (int) (20.0D / aps));
    }
}
