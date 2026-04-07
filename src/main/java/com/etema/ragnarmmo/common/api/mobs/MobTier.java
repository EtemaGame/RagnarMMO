package com.etema.ragnarmmo.common.api.mobs;

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
