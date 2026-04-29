package com.etema.ragnarmmo.combat.balance;

public record TtkBand(int minHits, int maxHits) {
    public TtkBand {
        if (minHits < 1) {
            throw new IllegalArgumentException("minHits must be >= 1");
        }
        if (maxHits < minHits) {
            throw new IllegalArgumentException("maxHits must be >= minHits");
        }
    }

    public boolean contains(int hits) {
        return hits >= minHits && hits <= maxHits;
    }
}
