package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobConfigAccessTest {
    @Test
    void maxSeverityUsesCanonicalRankOrder() {
        assertEquals(MobRank.BOSS, MobConfigAccess.maxSeverity(MobRank.ELITE, MobRank.BOSS));
        assertEquals(MobRank.MVP, MobConfigAccess.maxSeverity(MobRank.MVP, MobRank.BOSS));
        assertEquals(MobRank.MINI_BOSS, MobConfigAccess.maxSeverity(MobRank.NORMAL, MobRank.MINI_BOSS));
    }

    @Test
    void rankChanceTableRollsDescendingRareRanks() {
        MobConfigAccess.RankChanceTable table = new MobConfigAccess.RankChanceTable(0.20D, 0.10D, 0.05D, 0.01D);

        assertEquals(MobRank.MVP, table.roll(0.005D));
        assertEquals(MobRank.BOSS, table.roll(0.02D));
        assertEquals(MobRank.MINI_BOSS, table.roll(0.08D));
        assertEquals(MobRank.ELITE, table.roll(0.20D));
        assertEquals(MobRank.NORMAL, table.roll(0.80D));
    }

    @Test
    void rankChanceTableRejectsImpossibleDistribution() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobConfigAccess.RankChanceTable(0.50D, 0.30D, 0.20D, 0.10D));
    }

    @Test
    void difficultyRuleParsesAndRejectsInvalidRules() {
        MobConfigAccess.DifficultyRule rule = MobConfigAccess.DifficultyRule.parse("min_level=70,min_rank=ELITE", false);

        assertEquals(70, rule.minLevel().orElseThrow());
        assertEquals(MobRank.ELITE, rule.minRank().orElseThrow());

        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("min_level=70,min_rank=NOPE", false));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("", true));
    }

    @Test
    void distanceBandRejectsMalformedInput() {
        assertEquals(1, MobConfigAccess.DistanceBand.parse("0-999=1-5").levelRange().min());
        assertThrows(IllegalArgumentException.class, () -> MobConfigAccess.DistanceBand.parse("0-999"));
    }
}
