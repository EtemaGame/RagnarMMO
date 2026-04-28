package com.etema.ragnarmmo.combat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BasicAttackOutcomeTest {

    @Test
    void rejectedOutcomePreservesValidationReasonAndCancelsVanilla() {
        ResolvedTargetCandidate target = ResolvedTargetCandidate.rejected(42, TargetRejectReason.TARGET_OUT_OF_RANGE);

        BasicAttackOutcome outcome = BasicAttackOutcome.rejected(
                BasicAttackSource.CLIENT_PACKET,
                CombatRejectReason.NO_VALID_TARGETS,
                true,
                List.of(target));

        assertTrue(outcome.rejected());
        assertFalse(outcome.accepted());
        assertTrue(outcome.shouldCancelVanilla());
        assertEquals(CombatRejectReason.NO_VALID_TARGETS, outcome.rejectReason());
        assertEquals(BasicAttackFailureReason.NONE, outcome.failureReason());
        assertEquals(TargetRejectReason.TARGET_OUT_OF_RANGE, outcome.targetResults().get(0).rejectReason());
        assertTrue(outcome.resolutions().isEmpty());
        assertFalse(outcome.fallbackUsed());
    }

    @Test
    void resolvedOutcomeTreatsMissAsAcceptedAttack() {
        CombatResolution miss = CombatResolution.miss(1, 2);

        BasicAttackOutcome outcome = BasicAttackOutcome.resolved(
                BasicAttackSource.SERVER_ATTACK_EVENT,
                List.of(miss),
                List.of(),
                false);

        assertTrue(outcome.accepted());
        assertFalse(outcome.rejected());
        assertTrue(outcome.shouldCancelVanilla());
        assertNull(outcome.rejectReason());
        assertEquals(BasicAttackFailureReason.NONE, outcome.failureReason());
        assertEquals(CombatHitResultType.MISS, outcome.resolutions().get(0).resultType());
        assertFalse(outcome.fallbackUsed());
    }

    @Test
    void infrastructureFallbackKeepsFailureReasonAndMinimumHit() {
        CombatResolution fallback = CombatResolution.hit(1, 2, 1.0D, 1.0D, false);

        BasicAttackOutcome outcome = BasicAttackOutcome.infrastructureFallback(
                BasicAttackSource.CLIENT_PACKET,
                BasicAttackFailureReason.MISSING_ATTACK_PROFILE,
                fallback,
                List.of());

        assertTrue(outcome.accepted());
        assertTrue(outcome.shouldCancelVanilla());
        assertNull(outcome.rejectReason());
        assertEquals(BasicAttackFailureReason.MISSING_ATTACK_PROFILE, outcome.failureReason());
        assertTrue(outcome.fallbackUsed());
        assertEquals(1.0D, outcome.resolutions().get(0).finalAmount(), 0.0001D);
        assertEquals(CombatHitResultType.HIT, outcome.resolutions().get(0).resultType());
    }

    @Test
    void dodgeResolutionIsDistinctFromMiss() {
        CombatResolution dodge = CombatResolution.dodge(1, 2);

        assertEquals(CombatHitResultType.DODGE, dodge.resultType());
        assertEquals(0.0D, dodge.finalAmount(), 0.0001D);
        assertTrue(dodge.missed());
        assertFalse(dodge.critical());
    }
}
