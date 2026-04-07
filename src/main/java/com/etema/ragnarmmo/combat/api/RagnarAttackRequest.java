package com.etema.ragnarmmo.combat.api;

import java.util.Collections;
import java.util.List;

/**
 * RagnarAttackRequest - Domain-level model of a basic combat request.
 * Decouples the netcode from the engine's validation and resolution.
 */
public record RagnarAttackRequest(
        int sequenceId,
        int comboIndex,
        boolean offHand,
        int selectedSlot,
        List<RagnarTargetCandidate> candidates
) {
    /**
     * Create an empty request.
     */
    public static RagnarAttackRequest empty(int sequenceId) {
        return new RagnarAttackRequest(sequenceId, 0, false, 0, Collections.emptyList());
    }
}
