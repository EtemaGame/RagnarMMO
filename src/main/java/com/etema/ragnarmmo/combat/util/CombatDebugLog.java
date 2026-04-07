package com.etema.ragnarmmo.combat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;

/**
 * Focused structured logger for combat-engine work.
 */
public final class CombatDebugLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("RagnarMMO/Combat");

    private CombatDebugLog() {
    }

    public static void logAttackRequest(CombatRequestContext ctx) {
        LOGGER.debug("ATTACK_REQUEST actor={} seq={} combo={} offHand={} slot={} candidates={}",
                actorName(ctx), ctx.sequenceId(), ctx.comboIndex(), ctx.offHand(), ctx.selectedSlot(),
                ctx.candidates() != null ? ctx.candidates().size() : 0);
    }

    public static void logSkillRequest(CombatRequestContext ctx) {
        LOGGER.debug("SKILL_REQUEST actor={} seq={} skill={} slot={} candidates={}",
                actorName(ctx), ctx.sequenceId(), ctx.skillId(), ctx.selectedSlot(),
                ctx.candidates() != null ? ctx.candidates().size() : 0);
    }

    public static void logValidationReject(CombatRequestContext ctx, String reason) {
        int seq = ctx != null ? ctx.sequenceId() : -1;
        String action = ctx != null ? String.valueOf(ctx.actionType()) : "UNKNOWN";
        LOGGER.debug("VALIDATION_REJECT actor={} seq={} action={} reason={}",
                actorName(ctx), seq, action, reason);
    }

    public static void logHitResolution(CombatResolution resolution) {
        LOGGER.debug("HIT_RESOLUTION attacker={} target={} type={} amount={}",
                resolution.attackerId(), resolution.targetId(), resolution.resultType(), resolution.finalAmount());
    }

    public static void logCooldownReject(String actorName, String action, long readyTick, long nowTick) {
        LOGGER.debug("COOLDOWN_REJECT actor={} action={} readyTick={} nowTick={}",
                actorName, action, readyTick, nowTick);
    }

    private static String actorName(CombatRequestContext ctx) {
        if (ctx == null || ctx.actor() == null || ctx.actor().getGameProfile() == null) {
            return "unknown";
        }
        String name = ctx.actor().getGameProfile().getName();
        return (name == null || name.isBlank()) ? "unknown" : name;
    }
}
