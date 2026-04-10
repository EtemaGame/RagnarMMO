package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

/**
 * Issue detected while resolving the new manual mob path into a runtime-final profile.
 */
public record ManualMobProfileIssue(
        Kind kind,
        String field,
        String message) {

    public enum Kind {
        MISSING_COVERAGE,
        INVALID,
        INCOMPLETE,
        DERIVATION_UNIMPLEMENTED
    }
}
