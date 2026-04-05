package com.etema.ragnarmmo.system.stats.progression;

import com.etema.ragnarmmo.common.config.RagnarConfigs;

/**
 * Status point gain rules for Base Level progression.
 * Pre-renewal default follows the classic curve:
 * going from level X to X + 1 grants floor(X / 5) + 3 points.
 */
public final class StatPointProgression {

    private StatPointProgression() {
    }

    public static int pointsForLevelUp(int currentBaseLevel, int flatFallback) {
        if (!RagnarConfigs.SERVER.progression.usePreRenewalStatPointCurve.get()) {
            return Math.max(0, flatFallback);
        }

        return Math.max(0, (currentBaseLevel / 5) + 3);
    }
}
