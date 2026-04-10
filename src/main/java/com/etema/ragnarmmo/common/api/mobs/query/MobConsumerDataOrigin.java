package com.etema.ragnarmmo.common.api.mobs.query;

/**
 * Diagnostic source marker for the shared mob consumer read surface during coexistence.
 *
 * <p>This enum exists only to indicate where normalized read data came from during migration.
 * It is not gameplay semantics.</p>
 */
public enum MobConsumerDataOrigin {
    NEW_RUNTIME_PROFILE,
    LEGACY_COMPATIBILITY
}
