package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

public record MobProfile(
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
        String race,
        String element,
        String size) {
}
