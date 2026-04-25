package com.etema.ragnarmmo.combat.api;

/**
 * RagnarTargetSource - Identifies the origin of a target candidate.
 * Used by the engine to prioritize and validate targets.
 */
public enum RagnarTargetSource {
    /**
     * Target directly under the player's crosshair (vanilla/standard).
     */
    CLIENT_AIM,

    /**
     * Target collected via a sweep or AOE logic on the client.
     */
    CLIENT_SWEEP,

    /**
     * Target suggested by a skill's automated targeting logic.
     */
    SKILL_AUTO,

    /**
     * Target resolved on the server when no direct client target is used.
     */
    SERVER_RESOLVED
}
