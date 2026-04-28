package com.etema.ragnarmmo.combat.api;

public enum TargetRejectReason {
    NONE,
    TARGET_NOT_FOUND,
    TARGET_NOT_LIVING,
    TARGET_DEAD,
    TARGET_INVULNERABLE,
    TARGET_OUT_OF_RANGE,
    SELF_TARGET,
    TARGET_REMOVED
}
