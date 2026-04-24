package com.etema.ragnarmmo.player.stats.progression;

public final class StatCost {
    private StatCost() {
    }

    public static int costToIncrease(int currentStat) {
        int targetLevel = currentStat + 1;
        if (targetLevel <= 0) {
            return 1;
        }
        int cost = ((targetLevel - 1) / 10) + 2;
        return Math.max(cost, 1);
    }

    public static int costForLevel(int statLevel) {
        if (statLevel <= 0) {
            return 1;
        }
        int cost = ((statLevel - 1) / 10) + 2;
        return Math.max(cost, 1);
    }
}
