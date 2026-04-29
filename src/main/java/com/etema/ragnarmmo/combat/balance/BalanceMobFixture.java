package com.etema.ragnarmmo.combat.balance;

import com.etema.ragnarmmo.mobs.profile.MobTier;

public record BalanceMobFixture(
        int level,
        MobTier tier,
        int hp,
        int atkMin,
        int atkMax,
        int def,
        int mdef,
        int hit,
        int flee,
        int crit,
        int aspd) {
    public BalanceMobFixture {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier must not be null");
        }
        if (hp < 1) {
            throw new IllegalArgumentException("hp must be >= 1");
        }
    }

    public static BalanceMobFixture forPlayer(BalancePlayerFixture player, MobTier tier) {
        return forPlayer(player, tier, false);
    }

    public static BalanceMobFixture forPlayerMagic(BalancePlayerFixture player, MobTier tier) {
        return forPlayer(player, tier, true);
    }

    private static BalanceMobFixture forPlayer(BalancePlayerFixture player, MobTier tier, boolean magic) {
        int level = Math.max(1, player.level());
        BalanceMobFixture shell = defensiveShell(level, tier);
        double playerDamage = magic
                ? BalanceSimulator.expectedMagicDamage(player, shell)
                : BalanceSimulator.expectedPhysicalDamage(player, shell);
        TtkBand target = CombatBalanceContract.targetTtk(tier);
        int targetHits = (target.minHits() + target.maxHits()) / 2;
        int hp = Math.max(1, (int) Math.round(playerDamage * targetHits));
        return new BalanceMobFixture(
                level,
                tier,
                hp,
                shell.atkMin,
                shell.atkMax,
                shell.def,
                shell.mdef,
                shell.hit,
                shell.flee,
                shell.crit,
                shell.aspd);
    }

    private static BalanceMobFixture defensiveShell(int level, MobTier tier) {
        double multiplier = switch (tier) {
            case WEAK -> 0.75D;
            case NORMAL -> 1.0D;
            case ELITE -> 1.55D;
            case BOSS -> 3.0D;
        };
        int def = Math.max(0, (int) Math.round(level * 0.25D * multiplier));
        int mdef = Math.max(0, (int) Math.round(level * 0.18D * multiplier));
        int flee = Math.max(1, (int) Math.round(level * (0.85D + multiplier * 0.15D)));
        int hit = Math.max(1, (int) Math.round(level * (1.25D + multiplier * 0.2D)));
        int atkMin = Math.max(1, (int) Math.round((level * 1.0D + 4.0D) * multiplier));
        int atkMax = Math.max(atkMin, (int) Math.round((level * 1.4D + 8.0D) * multiplier));
        int crit = Math.max(0, (int) Math.round(multiplier));
        int aspd = Math.max(1, (int) Math.round(145 + level * 0.1D + multiplier));
        return new BalanceMobFixture(level, tier, 1, atkMin, atkMax, def, mdef, hit, flee, crit, aspd);
    }
}
