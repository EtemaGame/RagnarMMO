package com.etema.ragnarmmo.system.mobstats.world;

import java.util.Locale;
import java.util.Optional;

public enum BossSpawnSource {
    NATURAL,
    STRUCTURE,
    ALTAR,
    EVENT,
    SUMMON,
    DEBUG;

    public boolean isControlled() {
        return this != NATURAL;
    }

    public static Optional<BossSpawnSource> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(BossSpawnSource.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
