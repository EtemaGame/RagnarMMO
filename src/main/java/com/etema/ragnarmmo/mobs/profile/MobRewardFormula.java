package com.etema.ragnarmmo.mobs.profile;

public final class MobRewardFormula {
    private MobRewardFormula() {
    }

    public static int expectedHits(MobTier tier) {
        return switch (tier == null ? MobTier.NORMAL : tier) {
            case WEAK -> 6;
            case NORMAL -> 12;
            case ELITE -> 28;
            case BOSS -> 90;
        };
    }

    public static int baseExp(int level, MobTier tier) {
        int safeLevel = Math.max(1, level);
        MobTier safeTier = tier == null ? MobTier.NORMAL : tier;
        double tierMultiplier = switch (safeTier) {
            case WEAK -> 0.7D;
            case NORMAL -> 1.0D;
            case ELITE -> 2.4D;
            case BOSS -> 8.0D;
        };
        return Math.max(1, (int) Math.round((safeLevel * 4.0D + 8.0D) * expectedHits(safeTier) * tierMultiplier / 10.0D));
    }

    public static int jobExp(int level, MobTier tier) {
        return Math.max(1, (int) Math.round(baseExp(level, tier) * 0.65D));
    }
}
