package com.etema.ragnarmmo.common.api.mobs.data.resolve;

/**
 * Declarative issue detected while resolving or validating authored mob definition data.
 */
public record MobDefinitionResolutionIssue(
        Kind kind,
        String field,
        String message) {

    public enum Kind {
        INVALID,
        INCOMPLETE
    }
}
