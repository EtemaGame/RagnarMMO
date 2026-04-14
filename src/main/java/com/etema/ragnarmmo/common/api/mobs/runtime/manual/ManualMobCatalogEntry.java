package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

public record ManualMobCatalogEntry(
        String entityTypeId,
        String namespace,
        String displayName,
        boolean internalCoverage,
        boolean datapackCoverage,
        String effectiveBackend,
        boolean enabled) {
}
