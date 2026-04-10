package com.etema.ragnarmmo.common.api.mobs.query;

/**
 * Optional final-combat-stats read block for server-side inspection during coexistence.
 *
 * <p>This is read-only integration data only. It is not a runtime authority and should be absent
 * rather than fabricated when a source path cannot normalize these values safely.</p>
 */
public record MobConsumerInspectionStatsView(
        int maxHp,
        int atkMin,
        int atkMax,
        int def,
        int mdef) {

    public MobConsumerInspectionStatsView {
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
    }
}
