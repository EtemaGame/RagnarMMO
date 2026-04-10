package com.etema.ragnarmmo.system.mobstats.world.read;

import com.etema.ragnarmmo.system.mobstats.world.BossSpawnSource;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal read-only listing view for one active boss world-state entry.
 *
 * <p>This view is meant for admin/tooling listing surfaces and stays separate from semantic mob
 * reads such as rank, taxonomy, or data origin.</p>
 */
public record MobWorldStateActiveEntryReadView(
        UUID entityUuid,
        String displayName,
        String dimensionId,
        int x,
        int y,
        int z,
        boolean activeRegistrationPresent,
        @Nullable String encounterKey,
        @Nullable BossSpawnSource spawnSource,
        @Nullable Integer respawnDelayTicks,
        boolean cooldownPresent,
        boolean cooldownReady,
        @Nullable Long nextAllowedGameTime,
        @Nullable Long lastDefeatedGameTime) {

    public MobWorldStateActiveEntryReadView {
        Objects.requireNonNull(entityUuid, "entityUuid");
        displayName = requireNonBlank(displayName, "displayName");
        dimensionId = requireNonBlank(dimensionId, "dimensionId");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
