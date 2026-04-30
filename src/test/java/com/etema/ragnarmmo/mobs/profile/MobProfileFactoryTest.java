package com.etema.ragnarmmo.mobs.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.DefaultProfile;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess.FormulaRules;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyMode;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyResult;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MobProfileFactoryTest {
    private final MobProfileFactory factory = new MobProfileFactory(
            new DefaultProfile("brute", "neutral", "medium", 60, 4, 8, 1, 1, 20, 10, 1, 150, 0.23D),
            new FormulaRules(8.0D, 1.0D, 0.4D, 0.35D, 0.25D, 1.2D, 0.9D, 0.1D, 0.001D, 0.42D));

    @Test
    void authoredMobProfileUsesAuthoredRewardValues() {
        AuthoredMobDefinition authored = authoredBaseline();

        MobProfile profile = factory.create(difficulty(8, MobRank.NORMAL), Optional.of(authored));

        assertEquals(MobTier.NORMAL, profile.tier());
        assertEquals(37, profile.baseExp());
        assertEquals(24, profile.jobExp());
        assertEquals("undead", profile.race());
        assertEquals(new RoBaseStats(6, 5, 8, 2, 7, 1), profile.baseStats());
        assertEquals(180, profile.maxHp());
        assertEquals(9, profile.atkMin());
        assertEquals(6, profile.matkMin());
    }

    @Test
    void proceduralMobProfileScalesFullCombatAndRewardProfile() {
        MobProfile normal = factory.create(difficulty(20, MobRank.NORMAL), Optional.empty());
        MobProfile elite = factory.create(difficulty(20, MobRank.ELITE), Optional.empty());

        assertEquals(MobTier.NORMAL, normal.tier());
        assertEquals(MobTier.ELITE, elite.tier());
        assertTrue(elite.maxHp() > normal.maxHp());
        assertTrue(elite.atkMin() > normal.atkMin());
        assertTrue(elite.matkMin() >= normal.matkMin());
        assertTrue(elite.def() >= normal.def());
        assertTrue(elite.mdef() >= normal.mdef());
        assertTrue(elite.hit() >= normal.hit());
        assertTrue(elite.flee() >= normal.flee());
        assertTrue(elite.crit() >= normal.crit());
        assertTrue(elite.aspd() >= normal.aspd());
        assertTrue(elite.baseExp() > normal.baseExp());
        assertTrue(elite.jobExp() > normal.jobExp());
        assertTrue(normal.baseStats().dex() > 1);
        assertTrue(elite.baseStats().dex() > normal.baseStats().dex());
    }

    @Test
    void proceduralRewardDerivesFromLevelTierAndExpectedTtk() {
        MobProfile weak = factory.create(difficulty(10, MobRank.NORMAL), Optional.of(weakIdentityOnly()));
        MobProfile boss = factory.create(difficulty(10, MobRank.BOSS), Optional.empty());

        assertEquals(MobRewardFormula.baseExp(10, MobTier.NORMAL), weak.baseExp());
        assertEquals(MobRewardFormula.jobExp(10, MobTier.NORMAL), weak.jobExp());
        assertTrue(boss.baseExp() > weak.baseExp());
        assertTrue(MobRewardFormula.expectedHits(MobTier.BOSS) > MobRewardFormula.expectedHits(MobTier.NORMAL));
    }

    @Test
    void authoredBaselineScalesFromBaseLevelToRuntimeLevel() {
        AuthoredMobDefinition authored = authoredBaseline();
        MobProfile baseline = factory.create(difficulty(8, MobRank.NORMAL), Optional.of(authored));
        MobProfile scaled = factory.create(difficulty(80, MobRank.NORMAL), Optional.of(authored));

        assertEquals(8, authored.baseLevel().orElseThrow());
        assertEquals(80, scaled.level());
        assertTrue(scaled.maxHp() > baseline.maxHp());
        assertTrue(scaled.atkMin() > baseline.atkMin());
        assertTrue(scaled.matkMin() > baseline.matkMin());
        assertTrue(scaled.def() > baseline.def());
        assertTrue(scaled.mdef() > baseline.mdef());
        assertTrue(scaled.hit() > baseline.hit());
        assertTrue(scaled.flee() > baseline.flee());
        assertTrue(scaled.baseExp() > baseline.baseExp());
        assertTrue(scaled.jobExp() > baseline.jobExp());
        assertTrue(scaled.baseStats().dex() > baseline.baseStats().dex());
    }

    @Test
    void authoredBaselineDoesNotFreezeRewardsAboveBaseLevel() {
        AuthoredMobDefinition authored = authoredBaseline();
        MobProfile scaled = factory.create(difficulty(80, MobRank.NORMAL), Optional.of(authored));

        assertEquals(MobRewardFormula.baseExp(80, MobTier.NORMAL), scaled.baseExp());
        assertEquals(MobRewardFormula.jobExp(80, MobTier.NORMAL), scaled.jobExp());
    }

    @Test
    void authoredTierOnlyOverridesWhenStrongerThanProceduralTier() {
        AuthoredMobDefinition authored = new AuthoredMobDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "elite_zombie"),
                Optional.of(MobRank.NORMAL),
                Optional.of(MobTier.ELITE),
                OptionalInt.of(8),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.of(180),
                OptionalInt.of(9),
                OptionalInt.of(14),
                OptionalInt.of(6),
                OptionalInt.of(11),
                OptionalInt.of(3),
                OptionalInt.of(1),
                OptionalInt.of(18),
                OptionalInt.of(16),
                OptionalInt.of(1),
                OptionalInt.of(145),
                OptionalDouble.of(0.21D));

        MobProfile profile = factory.create(difficulty(8, MobRank.NORMAL), Optional.of(authored));

        assertEquals(MobTier.ELITE, profile.tier());
    }

    private static DifficultyResult difficulty(int level, MobRank rank) {
        return new DifficultyResult(level, rank, 1, 99, Optional.empty(), Optional.empty(), Optional.empty(),
                DifficultyMode.STATIC);
    }

    private static AuthoredMobDefinition authoredBaseline() {
        return new AuthoredMobDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"),
                Optional.of(MobRank.NORMAL),
                Optional.of(MobTier.WEAK),
                OptionalInt.of(8),
                Optional.of(new RoBaseStats(6, 5, 8, 2, 7, 1)),
                Optional.of("undead"),
                Optional.of("undead"),
                Optional.of("medium"),
                OptionalInt.of(37),
                OptionalInt.of(24),
                OptionalInt.of(180),
                OptionalInt.of(9),
                OptionalInt.of(14),
                OptionalInt.of(6),
                OptionalInt.of(11),
                OptionalInt.of(3),
                OptionalInt.of(1),
                OptionalInt.of(18),
                OptionalInt.of(16),
                OptionalInt.of(1),
                OptionalInt.of(145),
                OptionalDouble.of(0.21D));
    }

    private static AuthoredMobDefinition weakIdentityOnly() {
        return new AuthoredMobDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "slime"),
                Optional.empty(),
                Optional.of(MobTier.WEAK),
                OptionalInt.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalDouble.empty());
    }
}
