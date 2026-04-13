package com.etema.ragnarmmo.common.api.mobs;

/**
 * Defines the authority model for mob statistics and behaviors during the migration period.
 */
public enum MobRuntimeAuthority {
    /**
     * The mob is completely migrated to the new manual/datapack model.
     * Legacy stats, sync, and capabilities are disabled.
     */
    STRICT_NEW_AUTHORITY,

    /**
     * The mob has a partial definition or coverage but is not yet ready for full strictness.
     * Legacy fallbacks are allowed but will be logged as debt.
     */
    TEMP_COMPAT,

    /**
     * The mob has no manual definition and relies entirely on the legacy system.
     */
    LEGACY_ONLY
}
