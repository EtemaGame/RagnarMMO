package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.api.CombatRejectReason;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.BasicAttackSource;
import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.combat.state.CombatActorState;

/**
 * Centralized server-side validation for combat requests.
 */
public class RagnarCombatValidationService {

    /**
     * Validates a basic attack request.
     * 
     * @return null if valid, or a CombatRejectReason if rejected.
     */
    public CombatRejectReason validateBasicAttack(CombatRequestContext ctx, CombatActorState actorState, long nowTick,
            RagnarCombatCooldownService cooldownService) {
        return validateBasicAttack(ctx, actorState, nowTick, cooldownService, BasicAttackSource.CLIENT_PACKET);
    }

    public CombatRejectReason validateBasicAttack(CombatRequestContext ctx, CombatActorState actorState, long nowTick,
            RagnarCombatCooldownService cooldownService, BasicAttackSource source) {
        if (ctx == null || ctx.actor() == null) {
            return CombatRejectReason.MISSING_ACTOR;
        }
        if (!ctx.actor().isAlive()) {
            return CombatRejectReason.ACTOR_DEAD;
        }
        if (ctx.actor().isSpectator()) {
            return CombatRejectReason.ACTOR_SPECTATOR;
        }
        if (ctx.actor().isRemoved()) {
            return CombatRejectReason.ACTOR_REMOVED;
        }
        if (actorState == null) {
            return CombatRejectReason.MISSING_ACTOR_STATE;
        }
        if (actorState.getCastState().isCasting(nowTick)) {
            return CombatRejectReason.BASIC_ATTACK_COOLDOWN;
        }
        
        // Anti-spam payload limits
        if (ctx.candidates() == null || ctx.candidates().isEmpty()) {
            return CombatRejectReason.NO_VALID_TARGETS;
        }
        if (ctx.candidates().size() > 16) {
            return CombatRejectReason.TOO_MANY_CANDIDATES;
        }

        // Domain validation
        if (source == BasicAttackSource.SERVER_ATTACK_EVENT) {
            if (nowTick <= actorState.getLastServerFallbackTick()) {
                return CombatRejectReason.STALE_SEQUENCE;
            }
        } else if (ctx.sequenceId() <= actorState.getLastClientPacketSequenceId()) {
            return CombatRejectReason.STALE_SEQUENCE;
        }
        if (ctx.comboIndex() < 0 || ctx.comboIndex() > 8) {
            return CombatRejectReason.INVALID_COMBO_INDEX;
        }
        if (!AttackHandResolver.isValidAttackHand(ctx.actor(), ctx.offHand())) {
            return CombatRejectReason.INVALID_ATTACK_HAND;
        }

        // ASPD check
        if (!cooldownService.canUseBasicAttack(actorState.getCooldowns(), nowTick)) {
            return CombatRejectReason.BASIC_ATTACK_COOLDOWN;
        }
        
        return null;
    }

    /**
     * Validates a skill use request.
     */
    public CombatRejectReason validateSkillRequest(CombatRequestContext ctx, CombatActorState actorState, long nowTick,
            RagnarCombatCooldownService cooldownService) {
        if (ctx == null || ctx.actor() == null) {
            return CombatRejectReason.MISSING_ACTOR;
        }
        if (!ctx.actor().isAlive()) {
            return CombatRejectReason.ACTOR_DEAD;
        }
        if (ctx.actor().isSpectator()) {
            return CombatRejectReason.ACTOR_SPECTATOR;
        }
        if (ctx.skillId() == null || ctx.skillId().isBlank()) {
            return CombatRejectReason.MISSING_ACTOR; // Should have been skill enum
        }
        if (actorState == null) {
            return CombatRejectReason.MISSING_ACTOR_STATE;
        }
        if (actorState.getCastState().isCasting(nowTick)) {
            return CombatRejectReason.SKILL_COOLDOWN;
        }
        if (ctx.sequenceId() <= actorState.getLastAcceptedSequenceId()) {
            return CombatRejectReason.STALE_SEQUENCE;
        }
        if (!cooldownService.canUseSkill(actorState.getCooldowns(), ctx.skillId(), nowTick)) {
            return CombatRejectReason.SKILL_COOLDOWN;
        }
        return null;
    }
}
