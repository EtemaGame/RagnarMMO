package com.etema.ragnarmmo.common.api.mobs;

public enum MobTier {
    NORMAL,
    ELITE,
    MINI_BOSS,
    BOSS,
    MVP;

    public boolean isEliteOrHigher() {
        return this != NORMAL;
    }

    public boolean isMiniBossOrHigher() {
        return this == MINI_BOSS || this == BOSS || this == MVP;
    }

    public boolean isBossLike() {
        return this == BOSS || this == MVP;
    }

    public boolean shouldPersistWorldState() {
        return isMiniBossOrHigher();
    }
}
