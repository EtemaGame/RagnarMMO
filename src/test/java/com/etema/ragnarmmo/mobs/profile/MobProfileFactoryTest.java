package com.etema.ragnarmmo.mobs.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
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
        AuthoredMobDefinition authored = new AuthoredMobDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"),
                Optional.of(MobTier.WEAK),
                Optional.of("undead"),
                Optional.of("undead"),
                Optional.of("medium"),
                OptionalInt.of(37),
                OptionalInt.of(24),
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

        MobProfile profile = factory.create(difficulty(7, MobRank.NORMAL), Optional.of(authored));

        assertEquals(MobTier.WEAK, profile.tier());
        assertEquals(37, profile.baseExp());
        assertEquals(24, profile.jobExp());
        assertEquals("undead", profile.race());
    }

    @Test
    void proceduralMobProfileScalesFullCombatAndRewardProfile() {
        MobProfile normal = factory.create(difficulty(20, MobRank.NORMAL), Optional.empty());
        MobProfile elite = factory.create(difficulty(20, MobRank.ELITE), Optional.empty());

        assertEquals(MobTier.NORMAL, normal.tier());
        assertEquals(MobTier.ELITE, elite.tier());
        assertTrue(elite.maxHp() > normal.maxHp());
        assertTrue(elite.atkMin() > normal.atkMin());
        assertTrue(elite.def() >= normal.def());
        assertTrue(elite.mdef() >= normal.mdef());
        assertTrue(elite.hit() >= normal.hit());
        assertTrue(elite.flee() >= normal.flee());
        assertTrue(elite.crit() >= normal.crit());
        assertTrue(elite.aspd() >= normal.aspd());
        assertTrue(elite.baseExp() > normal.baseExp());
        assertTrue(elite.jobExp() > normal.jobExp());
    }

    @Test
    void proceduralRewardDerivesFromLevelTierAndExpectedTtk() {
        MobProfile weak = factory.create(difficulty(10, MobRank.NORMAL), Optional.of(new AuthoredMobDefinition(
                ResourceLocation.fromNamespaceAndPath("minecraft", "slime"),
                Optional.of(MobTier.WEAK),
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
                OptionalDouble.empty())));
        MobProfile boss = factory.create(difficulty(10, MobRank.BOSS), Optional.empty());

        assertEquals(MobRewardFormula.baseExp(10, MobTier.WEAK), weak.baseExp());
        assertEquals(MobRewardFormula.jobExp(10, MobTier.WEAK), weak.jobExp());
        assertTrue(boss.baseExp() > weak.baseExp());
        assertTrue(MobRewardFormula.expectedHits(MobTier.BOSS) > MobRewardFormula.expectedHits(MobTier.NORMAL));
    }

    private static DifficultyResult difficulty(int level, MobRank rank) {
        return new DifficultyResult(level, rank, 1, 99, Optional.empty(), Optional.empty(), DifficultyMode.STATIC);
    }
}
