package com.etema.ragnarmmo.system.mobstats.integration;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;

/**
 * Bridge for optional RagnarStats support.
 *
 * In RagnarMMO this support is always present (single jar), so we rely only on the
 * API registration flags rather than ModList checks.
 */
final class RagnarBridge {
    private RagnarBridge() {}

    static boolean hasStatsSupport() {
        return RagnarCoreAPI.hasStatsAccessor();
    }

    static boolean hasComputeSupport() {
        return hasStatsSupport() && RagnarCoreAPI.hasComputeFunction();
    }
}






