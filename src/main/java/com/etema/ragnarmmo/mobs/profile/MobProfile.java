package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

public record MobProfile(
        int level,
        MobRank rank,
        MobTier tier,
        int maxHp,
        int atkMin,
        int atkMax,
        int def,
        int mdef,
        int hit,
        int flee,
        int crit,
        int aspd,
        double moveSpeed,
        int baseExp,
        int jobExp,
        String race,
        String element,
        String size) {
    public MobProfile {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        if (rank == null) {
            throw new IllegalArgumentException("rank must not be null");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier must not be null");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be > 0");
        }
        if (atkMin < 0) {
            throw new IllegalArgumentException("atkMin must be >= 0");
        }
        if (atkMax < atkMin) {
            throw new IllegalArgumentException("atkMax must be >= atkMin");
        }
        if (def < 0 || mdef < 0 || hit < 0 || flee < 0 || crit < 0) {
            throw new IllegalArgumentException("def, mdef, hit, flee, and crit must be >= 0");
        }
        if (aspd <= 0) {
            throw new IllegalArgumentException("aspd must be > 0");
        }
        if (moveSpeed <= 0.0D) {
            throw new IllegalArgumentException("moveSpeed must be > 0");
        }
        if (baseExp < 0 || jobExp < 0) {
            throw new IllegalArgumentException("baseExp and jobExp must be >= 0");
        }
        race = requireToken(race, "race");
        element = requireToken(element, "element");
        size = requireToken(size, "size");
    }

    private static String requireToken(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
