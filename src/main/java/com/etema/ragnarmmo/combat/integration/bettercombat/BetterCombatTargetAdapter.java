package com.etema.ragnarmmo.combat.integration.bettercombat;

import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import com.etema.ragnarmmo.combat.api.RagnarTargetSource;

/**
 * BetterCombatTargetAdapter - Translates Better Combat hit results into 
 * neutral RagnarTargetCandidate objects.
 */
public final class BetterCombatTargetAdapter {
    private BetterCombatTargetAdapter() {
    }

    /**
     * Map a single entity ID suggested by Better Combat into a candidate.
     */
    public static RagnarTargetCandidate fromEntityId(int entityId) {
        return new RagnarTargetCandidate(entityId, RagnarTargetSource.BETTER_COMBAT, 0.0D);
    }
}
