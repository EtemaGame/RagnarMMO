package com.etema.ragnarmmo.combat.balance;

import com.etema.ragnarmmo.mobs.profile.MobTier;

public final class CombatBalanceContract {
    public static final TtkBand WEAK_TTK = new TtkBand(5, 8);
    public static final TtkBand NORMAL_TTK = new TtkBand(10, 15);
    public static final TtkBand ELITE_TTK = new TtkBand(20, 40);

    private CombatBalanceContract() {
    }

    public static TtkBand targetTtk(MobTier tier) {
        return switch (tier == null ? MobTier.NORMAL : tier) {
            case WEAK -> WEAK_TTK;
            case NORMAL -> NORMAL_TTK;
            case ELITE -> ELITE_TTK;
            case BOSS -> throw new IllegalArgumentException("Boss TTK is measured by DPS windows, not simple hits");
        };
    }
}
