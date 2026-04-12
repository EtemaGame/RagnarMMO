package com.etema.ragnarmmo.combat.api;

/**
 * Lightweight target candidate descriptor passed from network layers into the
 * combat engine.
 */
public record CombatTargetCandidate(
        int entityId,
        String source,
        double distance,
        boolean fromClientExtendedHitbox) {

    public static CombatTargetCandidate clientExtended(int entityId, double distance) {
        return new CombatTargetCandidate(entityId, "client_extended", distance, true);
    }
}
