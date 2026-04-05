package com.etema.ragnarmmo.combat.bridge;

import java.util.List;

/**
 * Transport-safe bridge snapshot extracted from Better Combat integration
 * points.
 */
public record BetterCombatAttackSnapshot(
        int comboIndex,
        boolean offHand,
        String animationId,
        double upswingRatio,
        double attackRange,
        List<Integer> candidateTargetIds) {
}
