package com.etema.ragnarmmo.combat.engine;

import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.state.CombatActorState;

/**
 * Centralized server-side validation for combat requests.
 */
public class RagnarCombatValidationService {

    public String validateBasicAttack(CombatRequestContext ctx, CombatActorState actorState, long nowTick,
            RagnarCombatCooldownService cooldownService) {
        if (ctx == null || ctx.actor() == null) {
            return "missing_actor";
        }
        if (!ctx.actor().isAlive()) {
            return "actor_dead";
        }
        if (actorState == null) {
            return "missing_actor_state";
        }
        if (ctx.sequenceId() <= actorState.getLastAcceptedSequenceId()) {
            return "stale_sequence";
        }
        if (!cooldownService.canUseBasicAttack(actorState.getCooldowns(), nowTick)) {
            return "basic_attack_cooldown";
        }
        return null;
    }

    public String validateSkillRequest(CombatRequestContext ctx, CombatActorState actorState, long nowTick,
            RagnarCombatCooldownService cooldownService) {
        if (ctx == null || ctx.actor() == null) {
            return "missing_actor";
        }
        if (!ctx.actor().isAlive()) {
            return "actor_dead";
        }
        if (ctx.skillId() == null || ctx.skillId().isBlank()) {
            return "missing_skill_id";
        }
        if (actorState == null) {
            return "missing_actor_state";
        }
        if (ctx.sequenceId() <= actorState.getLastAcceptedSequenceId()) {
            return "stale_sequence";
        }
        if (!cooldownService.canUseSkill(actorState.getCooldowns(), ctx.skillId(), nowTick)) {
            return "skill_cooldown";
        }
        return null;
    }
}
