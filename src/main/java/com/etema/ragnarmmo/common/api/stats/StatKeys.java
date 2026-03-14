package com.etema.ragnarmmo.common.api.stats;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public enum StatKeys {
    STR, AGI, VIT, INT, DEX, LUK;

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
        return values[rng.nextInt(values.length)];
    }
}






