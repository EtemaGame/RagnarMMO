package com.etema.ragnarmmo.combat.api;

/**
 * Lightweight target candidate descriptor passed from bridge/network layers into
 * the combat engine.
 */
public record CombatTargetCandidate(
        int entityId,
        String source,
        double distance,
        boolean fromBetterCombatHitbox) {

    public static CombatTargetCandidate betterCombat(int entityId, double distance) {
        return new CombatTargetCandidate(entityId, "better_combat", distance, true);
    }
}
