package com.etema.ragnarmmo.mobs.profile;

import com.etema.ragnarmmo.mobs.difficulty.MobDifficultyResolver;

public final class MobProfileFactory {
    public MobProfile create(MobDifficultyResolver.Result difficulty) {
        int level = Math.max(1, difficulty.level());
        int maxHp = 20 + (level * 8);
        int atkMin = 2 + level;
        int atkMax = atkMin + Math.max(1, level / 2);
        int def = Math.max(0, level / 3);
        int mdef = Math.max(0, level / 4);
        int hit = 10 + (level * 2);
        int flee = 5 + level;
        int crit = Math.max(1, level / 10);
        int aspd = 150 + Math.min(40, level / 2);
        double moveSpeed = Math.min(0.36D, 0.20D + (level * 0.002D));
        return new MobProfile(level, difficulty.rank(), maxHp, atkMin, atkMax, def, mdef, hit, flee, crit, aspd,
                moveSpeed, "unknown", "neutral", "medium");
    }
}
