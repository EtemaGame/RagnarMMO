package com.etema.ragnarmmo.combat.balance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.mobs.profile.MobTier;
import org.junit.jupiter.api.Test;

class CombatBalanceFixtureTest {
    private static final int[] LEVELS = {1, 10, 25, 50, 99};

    @Test
    void physicalBuildsStayInsideHitsToKillBands() {
        for (int level : LEVELS) {
            for (BalanceBuildType build : new BalanceBuildType[] {
                    BalanceBuildType.STR,
                    BalanceBuildType.AGI,
                    BalanceBuildType.VIT
            }) {
                BalancePlayerFixture player = BalancePlayerFixture.at(level, build);
                assertTtkInBand(player, MobTier.WEAK, false);
                assertTtkInBand(player, MobTier.NORMAL, false);
                assertTtkInBand(player, MobTier.ELITE, false);
            }
        }
    }

    @Test
    void intBuildStaysInsideMagicHitsToKillBands() {
        for (int level : LEVELS) {
            BalancePlayerFixture player = BalancePlayerFixture.at(level, BalanceBuildType.INT);
            assertTtkInBand(player, MobTier.WEAK, true);
            assertTtkInBand(player, MobTier.NORMAL, true);
            assertTtkInBand(player, MobTier.ELITE, true);
        }
    }

    @Test
    void buildsHaveDistinctIdentities() {
        BalancePlayerFixture str = BalancePlayerFixture.at(50, BalanceBuildType.STR);
        BalancePlayerFixture agi = BalancePlayerFixture.at(50, BalanceBuildType.AGI);
        BalancePlayerFixture intel = BalancePlayerFixture.at(50, BalanceBuildType.INT);
        BalancePlayerFixture vit = BalancePlayerFixture.at(50, BalanceBuildType.VIT);

        assertTrue(str.averageAttack() > agi.averageAttack());
        assertTrue(agi.flee() > str.flee());
        assertTrue(intel.averageMagicAttack() > str.averageMagicAttack());
        assertTrue(vit.vit() > str.vit());
    }

    @Test
    void eliteMobsDifferByProfileCompositionNotOnlyHp() {
        BalancePlayerFixture player = BalancePlayerFixture.at(50, BalanceBuildType.STR);
        BalanceMobFixture normal = BalanceMobFixture.forPlayer(player, MobTier.NORMAL);
        BalanceMobFixture elite = BalanceMobFixture.forPlayer(player, MobTier.ELITE);

        assertTrue(elite.hp() > normal.hp());
        assertTrue(elite.atkMin() > normal.atkMin());
        assertTrue(elite.atkMax() > normal.atkMax());
        assertTrue(elite.def() > normal.def());
        assertTrue(elite.mdef() >= normal.mdef());
        assertTrue(elite.hit() > normal.hit());
        assertTrue(elite.flee() > normal.flee());
        assertTrue(elite.crit() >= normal.crit());
        assertTrue(elite.aspd() >= normal.aspd());
    }

    private static void assertTtkInBand(BalancePlayerFixture player, MobTier tier, boolean magic) {
        BalanceMobFixture mob = magic
                ? BalanceMobFixture.forPlayerMagic(player, tier)
                : BalanceMobFixture.forPlayer(player, tier);
        double damage = magic
                ? BalanceSimulator.expectedMagicDamage(player, mob)
                : BalanceSimulator.expectedPhysicalDamage(player, mob);
        int hits = BalanceSimulator.hitsToKill(damage, mob.hp());

        assertTrue(
                CombatBalanceContract.targetTtk(tier).contains(hits),
                () -> player.build() + " level " + player.level() + " vs " + tier + " took " + hits + " hits");
    }
}
