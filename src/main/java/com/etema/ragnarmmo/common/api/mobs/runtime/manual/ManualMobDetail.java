package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import org.jetbrains.annotations.Nullable;

/**
 * Rich server-authoritative detail object for a specific mob type.
 * Provides separate layers for internal data, datapack coverage, and final effective profile.
 */
public record ManualMobDetail(
        @Nullable InternalManualMobEntry internalEntry,
        @Nullable ComputedMobProfile effectiveProfile,
        boolean internalPresent,
        boolean internalEnabled,
        boolean datapackCoverage,
        String effectiveBackend,
        boolean manualEffective,
        String scalingMode,
        String backendReason,
        boolean canEdit,
        boolean canDelete,
        boolean canCreate) {
}
