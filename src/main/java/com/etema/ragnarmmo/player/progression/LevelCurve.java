package com.etema.ragnarmmo.player.progression;

@FunctionalInterface
public interface LevelCurve {
    int expToNext(int level);
}
