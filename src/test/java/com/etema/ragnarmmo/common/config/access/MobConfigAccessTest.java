package com.etema.ragnarmmo.common.config.access;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobConfigAccessTest {
    @Test
    void maxSeverityUsesCanonicalRankOrder() {
        assertEquals(MobRank.BOSS, MobConfigAccess.maxSeverity(MobRank.ELITE, MobRank.BOSS));
        assertEquals(MobRank.BOSS, MobConfigAccess.maxSeverity(MobRank.MINI_BOSS, MobRank.BOSS));
        assertEquals(MobRank.MINI_BOSS, MobConfigAccess.maxSeverity(MobRank.NORMAL, MobRank.MINI_BOSS));
    }

    @Test
    void mobRankDoesNotContainMvp() {
        String removedRankName = "M" + "VP";
        assertFalse(Arrays.stream(MobRank.values()).anyMatch(rank -> removedRankName.equals(rank.name())));
    }

    @Test
    void rankChanceTableOnlyRollsNormalOrElite() {
        MobConfigAccess.RankChanceTable alwaysNormal = new MobConfigAccess.RankChanceTable(0.0D);
        MobConfigAccess.RankChanceTable alwaysElite = new MobConfigAccess.RankChanceTable(1.0D);

        assertEquals(MobRank.NORMAL, alwaysNormal.roll(0.0D));
        assertEquals(MobRank.NORMAL, alwaysNormal.roll(0.999D));
        assertEquals(MobRank.ELITE, alwaysElite.roll(0.0D));
        assertEquals(MobRank.ELITE, alwaysElite.roll(0.999D));
    }

    @Test
    void rankChanceTableRejectsInvalidEliteChance() {
        assertThrows(IllegalArgumentException.class, () -> new MobConfigAccess.RankChanceTable(-0.01D));
        assertThrows(IllegalArgumentException.class, () -> new MobConfigAccess.RankChanceTable(1.01D));
    }

    @Test
    void difficultyRuleParsesAndRejectsInvalidRules() {
        MobConfigAccess.DifficultyRule rule = MobConfigAccess.DifficultyRule.parse(
                "min_level=70,min_rank=ELITE",
                MobConfigAccess.RuleScope.STRUCTURE);

        assertEquals(70, rule.minLevel().orElseThrow());
        assertEquals(MobRank.ELITE, rule.minRank().orElseThrow());

        MobConfigAccess.DifficultyRule biomeRule = MobConfigAccess.DifficultyRule.parse(
                "min_level=35,min_rank=NORMAL",
                MobConfigAccess.RuleScope.BIOME);
        assertEquals(35, biomeRule.minLevel().orElseThrow());
        assertEquals(MobRank.NORMAL, biomeRule.minRank().orElseThrow());

        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("min_level=70,min_rank=NOPE",
                        MobConfigAccess.RuleScope.STRUCTURE));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("", MobConfigAccess.RuleScope.SPECIAL_MOB));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("min_rank=MINI_BOSS",
                        MobConfigAccess.RuleScope.STRUCTURE));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("rank=ELITE",
                        MobConfigAccess.RuleScope.SPECIAL_MOB));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("min_rank=BOSS",
                        MobConfigAccess.RuleScope.SPECIAL_MOB));
        assertThrows(IllegalArgumentException.class,
                () -> MobConfigAccess.DifficultyRule.parse("rank=ELITE",
                        MobConfigAccess.RuleScope.BIOME));

        MobConfigAccess.DifficultyRule specialRule = MobConfigAccess.DifficultyRule.parse(
                "rank=MINI_BOSS,min_level=80",
                MobConfigAccess.RuleScope.SPECIAL_MOB);
        assertEquals(MobRank.MINI_BOSS, specialRule.fixedRank().orElseThrow());
    }

    @Test
    void distanceBandRejectsMalformedInput() {
        assertEquals(1, MobConfigAccess.DistanceBand.parse("0-999=1-5").levelRange().min());
        assertThrows(IllegalArgumentException.class, () -> MobConfigAccess.DistanceBand.parse("0-999"));
    }
}
