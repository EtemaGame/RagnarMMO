package com.etema.ragnarmmo.combat.balance;

public record BalancePlayerFixture(
        int level,
        BalanceBuildType build,
        int str,
        int agi,
        int vit,
        int intel,
        int dex,
        int luk,
        double atkMin,
        double atkMax,
        double matkMin,
        double matkMax,
        double hit,
        double flee) {
    public BalancePlayerFixture {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        if (build == null) {
            throw new IllegalArgumentException("build must not be null");
        }
    }

    public static BalancePlayerFixture at(int level, BalanceBuildType build) {
        int safeLevel = Math.max(1, level);
        int budget = Math.max(0, safeLevel - 1);
        int str = 5;
        int agi = 5;
        int vit = 5;
        int intel = 5;
        int dex = 5;
        int luk = 1;

        switch (build) {
            case STR -> {
                str += budget * 2;
                dex += budget;
                vit += budget / 2;
            }
            case AGI -> {
                agi += budget * 2;
                dex += budget;
                str += budget / 2;
            }
            case INT -> {
                intel += budget * 2;
                dex += budget;
                vit += budget / 3;
            }
            case VIT -> {
                vit += budget * 2;
                str += budget;
                dex += budget / 2;
            }
        }

        double weapon = 4.0D + safeLevel * 1.65D;
        double atkCenter = weapon + str * 1.35D + dex * 0.45D;
        double matkCenter = 3.0D + safeLevel * 1.45D + intel * 1.55D + dex * 0.25D;
        double hit = safeLevel + dex + luk * 0.3D + 25.0D;
        double flee = safeLevel + agi + luk * 0.2D;
        return new BalancePlayerFixture(
                safeLevel,
                build,
                str,
                agi,
                vit,
                intel,
                dex,
                luk,
                Math.max(1.0D, atkCenter * 0.9D),
                Math.max(1.0D, atkCenter * 1.1D),
                Math.max(1.0D, matkCenter * 0.9D),
                Math.max(1.0D, matkCenter * 1.1D),
                hit,
                flee);
    }

    public double averageAttack() {
        return (atkMin + atkMax) * 0.5D;
    }

    public double averageMagicAttack() {
        return (matkMin + matkMax) * 0.5D;
    }
}
