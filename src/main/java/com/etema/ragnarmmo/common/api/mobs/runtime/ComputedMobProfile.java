package com.etema.ragnarmmo.common.api.mobs.runtime;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Final runtime profile for one mob encounter instance in the new mob system.
 *
 * <p>This is the only new runtime-final authority. It is intentionally separate from declarative
 * loading models such as {@code MobDefinition} and {@code ResolvedMobDefinition}, and from older
 * runtime-side containers.</p>
 *
 * <p>This type carries resolved semantic values only. It does not apply attributes, perform sync,
 * or imply any runtime wiring on its own.</p>
 *
 * <p>Some runtime-final combat values, such as optional base combat attributes derived safely from
 * the manual path, may be embedded here when available. Absent optional blocks must not be
 * fabricated by compatibility paths.</p>
 */
public record ComputedMobProfile(
        int level,
        MobRank rank,
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
        @Nullable ComputedMobBaseStats baseCombatStats,
        String race,
        String element,
        String size) {

    public ComputedMobProfile {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }

        rank = Objects.requireNonNull(rank, "rank");

        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be > 0");
        }
        if (atkMin < 0) {
            throw new IllegalArgumentException("atkMin must be >= 0");
        }
        if (atkMax < atkMin) {
            throw new IllegalArgumentException("atkMax must be >= atkMin");
        }
        if (def < 0) {
            throw new IllegalArgumentException("def must be >= 0");
        }
        if (mdef < 0) {
            throw new IllegalArgumentException("mdef must be >= 0");
        }
        if (hit < 0) {
            throw new IllegalArgumentException("hit must be >= 0");
        }
        if (flee < 0) {
            throw new IllegalArgumentException("flee must be >= 0");
        }
        if (crit < 0) {
            throw new IllegalArgumentException("crit must be >= 0");
        }
        if (aspd <= 0) {
            throw new IllegalArgumentException("aspd must be > 0");
        }
        if (moveSpeed <= 0.0D) {
            throw new IllegalArgumentException("moveSpeed must be > 0");
        }

        race = requireNonBlank(race, "race");
        element = requireNonBlank(element, "element");
        size = requireNonBlank(size, "size");
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
