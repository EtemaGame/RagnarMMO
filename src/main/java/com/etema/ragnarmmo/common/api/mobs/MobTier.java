package com.etema.ragnarmmo.common.api.mobs;

/**
 * Legacy compatibility category from the old mob-stats pipeline.
 *
 * <p>{@code MobTier} remains in the codebase because several legacy systems still persist or
 * consume it. It is not the base semantic encounter model of the new architecture; new migration
 * work should prefer {@link MobRank}, the shared read surface, and the separate world-state
 * boundary instead of treating this enum as authoritative new semantics.</p>
 *
 * <p>This enum still mixes encounter intensity with some old compatibility behavior such as
 * boss-like checks and world-state persistence heuristics. That coupling is legacy-only and should
 * not be expanded in new code.</p>
 */
public enum MobTier {
    NORMAL(1.0, 1.0, 1.0, 1.0),
    ELITE(5.0, 1.5, 1.2, 5.0),
    MINI_BOSS(20.0, 2.5, 1.5, 20.0),
    BOSS(100.0, 5.0, 2.0, 100.0),
    MVP(500.0, 10.0, 3.0, 1000.0);

    private final double hpMult;
    private final double dmgMult;
    private final double defMult;
    private final double expMult;

    MobTier(double hpMult, double dmgMult, double defMult, double expMult) {
        this.hpMult = hpMult;
        this.dmgMult = dmgMult;
        this.defMult = defMult;
        this.expMult = expMult;
    }

    public double getHpMultiplier() { return hpMult; }
    public double getDamageMultiplier() { return dmgMult; }
    public double getDefenseMultiplier() { return defMult; }
    public double getExpMultiplier() { return expMult; }

    public boolean isEliteOrHigher() {
        return this != NORMAL;
    }

    public boolean isMiniBossOrHigher() {
        return this == MINI_BOSS || this == BOSS || this == MVP;
    }

    public boolean isBoss() {
        return this == BOSS || this == MVP;
    }

    /**
     * Legacy compatibility helper for old boss-like checks.
     *
     * <p>This should not be treated as the new encounter/lifecycle model for migrated paths.</p>
     */
    public boolean isBossLike() {
        return this == MINI_BOSS || this == BOSS || this == MVP;
    }

    /**
     * Legacy persistence heuristic used by the old world-state path.
     *
     * <p>New code should prefer the explicit world-state read/write boundaries instead of deriving
     * lifecycle policy from {@code MobTier}.</p>
     */
    public boolean shouldPersistWorldState() {
        return isMiniBossOrHigher();
    }
    
    public String getPrefix() {
        return switch (this) {
            case ELITE -> "[Elite] ";
            case MINI_BOSS -> "[Mini-Boss] ";
            case BOSS -> "[Boss] ";
            case MVP -> "[MVP] ";
            default -> "";
        };
    }
}
