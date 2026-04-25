package com.etema.ragnarmmo.common.api.mobs.data.resolve;

import com.etema.ragnarmmo.common.api.mobs.data.ResolvedMobDefinition;

import java.util.List;

/**
 * Pure declarative resolution result for authored mob data.
 *
 * <p>This result does not imply datapack IO, runtime integration, or stat derivation.</p>
 */
public record MobDefinitionResolutionResult(
        ResolvedMobDefinition definition,
        List<MobDefinitionResolutionIssue> issues) {

    public MobDefinitionResolutionResult {
        issues = List.copyOf(issues);
    }

    public boolean hasInvalidIssues() {
        return issues.stream().anyMatch(issue -> issue.kind() == MobDefinitionResolutionIssue.Kind.INVALID);
    }

    public boolean isComplete() {
        return issues.stream().noneMatch(issue -> issue.kind() == MobDefinitionResolutionIssue.Kind.INCOMPLETE);
    }
}
