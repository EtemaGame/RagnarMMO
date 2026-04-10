package com.etema.ragnarmmo.common.api.mobs.runtime.resolve;

import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure manual-path resolution result for the new mob runtime profile.
 *
 * <p>This result does not imply spawn integration, attribute application, or client sync.</p>
 */
public record ManualMobProfileResolutionResult(
        @Nullable ComputedMobProfile profile,
        List<ManualMobProfileIssue> issues) {

    public ManualMobProfileResolutionResult {
        issues = List.copyOf(issues);
    }

    public boolean isSuccess() {
        return profile != null && issues.isEmpty();
    }
}
