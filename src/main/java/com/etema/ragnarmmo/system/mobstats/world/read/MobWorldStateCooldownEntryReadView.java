package com.etema.ragnarmmo.system.mobstats.world.read;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal read-only listing view for one cooldown/history entry keyed by encounter key.
 */
public record MobWorldStateCooldownEntryReadView(
        String encounterKey,
        String entityTypeId,
        String displayName,
        String dimensionId,
        boolean activeRegistrationPresent,
        boolean cooldownPresent,
        boolean cooldownReady,
        @Nullable Long nextAllowedGameTime,
        @Nullable Long lastDefeatedGameTime) {

    public MobWorldStateCooldownEntryReadView {
        encounterKey = requireNonBlank(encounterKey, "encounterKey");
        entityTypeId = requireNonBlank(entityTypeId, "entityTypeId");
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
