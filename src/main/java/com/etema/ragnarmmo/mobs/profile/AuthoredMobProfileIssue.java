package com.etema.ragnarmmo.mobs.profile;

public record AuthoredMobProfileIssue(
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
