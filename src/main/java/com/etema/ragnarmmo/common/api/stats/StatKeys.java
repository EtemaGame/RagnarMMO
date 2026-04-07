package com.etema.ragnarmmo.common.api.stats;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * Core stats used for character progression and combat math.
 */
public enum StatKeys {
    STR, AGI, VIT, INT, DEX, LUK, LEVEL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<StatKeys> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(id.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static StatKeys random(Random rng) {
        StatKeys[] values = values();
        // Exclude LEVEL from random attribute roll
        return values[rng.nextInt(values.length - 1)];
    }
}
