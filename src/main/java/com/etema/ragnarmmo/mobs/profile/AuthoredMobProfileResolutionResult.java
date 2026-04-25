package com.etema.ragnarmmo.mobs.profile;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AuthoredMobProfileResolutionResult(
        @Nullable MobProfile profile,
        List<AuthoredMobProfileIssue> issues) {

    public AuthoredMobProfileResolutionResult {
        issues = List.copyOf(issues);
    }

    public boolean isSuccess() {
        return profile != null && issues.isEmpty();
    }
}
