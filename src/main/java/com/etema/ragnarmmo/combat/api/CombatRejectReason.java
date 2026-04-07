package com.etema.ragnarmmo.combat.api;

/**
 * CombatRejectReason - Standardized reasons for rejecting a combat request.
 * Helps with observability and client-side feedback.
 */
public enum CombatRejectReason {
    /**
     * Missing mandatory request fields (e.g., actor is null).
     */
    MISSING_ACTOR,

    /**
     * The attacker is already dead.
     */
    ACTOR_DEAD,

    /**
     * The attacker is in spectator mode.
     */
    ACTOR_SPECTATOR,

    /**
     * The attacker entity has been removed from the world.
     */
    ACTOR_REMOVED,

    /**
     * No persistent combat state found for the actor.
     */
    MISSING_ACTOR_STATE,

    /**
     * Packet sequence ID is older than or equal to the last accepted one.
     */
    STALE_SEQUENCE,

    /**
     * Attack speed cooldown has not yet expired.
     */
    BASIC_ATTACK_COOLDOWN,

    /**
     * Global skill cast delay is active.
     */
    SKILL_COOLDOWN,

    /**
     * Too many target candidates provided in a single request (anti-spam).
     */
    TOO_MANY_CANDIDATES,

    /**
     * The combo index requested is out of the valid range (e.g., negative or >8).
     */
    INVALID_COMBO_INDEX,

    /**
     * The selected inventory slot does not match the server's record.
     */
    INVALID_SLOT,

    /**
     * The requested hand (off-hand) is not valid for this attack or weapon.
     */
    INVALID_OFFHAND,

    /**
     * No valid LivingEntity targets were found in the candidate list.
     */
    NO_VALID_TARGETS
}
