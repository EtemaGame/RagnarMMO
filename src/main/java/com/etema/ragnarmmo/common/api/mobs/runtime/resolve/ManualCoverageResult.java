package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

import com.etema.ragnarmmo.common.config.RagnarConfigs;

/**
 * Manual profile coverage signal used to keep "effective scaling mode" decisions separate from data presence.
 */
public record ManualCoverageResult(
        boolean covered,
        RagnarConfigs.ManualMobBackend backend,
        boolean profilePresent,
        String reason) {
}
