package com.etema.ragnarmmo.combat.api;

public enum BasicAttackFailureReason {
    NONE,
    MISSING_ATTACKER_STATS,
    MISSING_ATTACK_PROFILE,
    NO_RESOLUTION_PRODUCED,
    TARGET_DAMAGE_REJECTED,
    INTERNAL_ERROR
}
