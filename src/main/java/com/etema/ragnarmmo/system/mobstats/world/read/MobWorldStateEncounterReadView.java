package com.etema.ragnarmmo.system.mobstats.world.read;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal read-only world-state/history view keyed by encounter key.
 *
 * <p>This view answers lifecycle, cooldown, and minimal defeat-history questions for one encounter
 * key without mixing those answers into the shared semantic mob read surface.</p>
 */
public record MobWorldStateEncounterReadView(
        String encounterKey,
        boolean activeRegistrationPresent,
        boolean cooldownPresent,
        boolean cooldownReady,
        @Nullable Long nextAllowedGameTime,
        @Nullable Long lastDefeatedGameTime) {

    public MobWorldStateEncounterReadView {
        encounterKey = requireNonBlank(encounterKey, "encounterKey");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
