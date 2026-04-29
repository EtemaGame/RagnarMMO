package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

public enum MobTier {
    WEAK,
    NORMAL,
    ELITE,
    BOSS;

    public static MobTier fromRank(MobRank rank) {
        if (rank == null) {
            return NORMAL;
        }
        return switch (rank) {
            case NORMAL -> NORMAL;
            case ELITE -> ELITE;
            case MINI_BOSS, BOSS -> BOSS;
        };
    }
}
