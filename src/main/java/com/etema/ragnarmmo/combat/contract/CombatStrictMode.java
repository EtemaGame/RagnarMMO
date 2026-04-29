package com.etema.ragnarmmo.combat.contract;

/**
 * Controls missing-profile behavior without silently reintroducing vanilla
 * balance inputs.
 */
public enum CombatStrictMode {
    DEV,
    PROD;

    public static CombatStrictMode current() {
        return Boolean.getBoolean("ragnarmmo.combat.strict") ? DEV : PROD;
    }
}
