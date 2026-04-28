package com.etema.ragnarmmo.combat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etema.ragnarmmo.combat.api.BasicAttackFailureReason;
import com.etema.ragnarmmo.combat.api.BasicAttackOutcome;
import com.etema.ragnarmmo.combat.api.BasicAttackSource;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

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

    public static void logAttackPacing(CombatRequestContext ctx, boolean dualWield, double aps, int intervalTicks) {
        LOGGER.debug("ATTACK_PACING actor={} offHand={} dual={} aps={} interval={}",
                actorName(ctx), ctx != null && ctx.offHand(), dualWield, aps, intervalTicks);
    }

    public static void logCooldownReject(String actorName, String action, long readyTick, long nowTick) {
        LOGGER.debug("COOLDOWN_REJECT actor={} action={} readyTick={} nowTick={}",
                actorName, action, readyTick, nowTick);
    }

    public static void logBasicAttackOutcome(BasicAttackOutcome outcome) {
        if (outcome == null) {
            LOGGER.debug("BASIC_ATTACK_OUTCOME null");
            return;
        }
        LOGGER.debug("BASIC_ATTACK_OUTCOME source={} accepted={} cancelVanilla={} reject={} failure={} resolutions={} targets={} fallback={}",
                outcome.source(), outcome.accepted(), outcome.shouldCancelVanilla(), outcome.rejectReason(),
                outcome.failureReason(), outcome.resolutions().size(), outcome.targetResults().size(),
                outcome.fallbackUsed());
        outcome.targetResults().forEach(target -> LOGGER.debug(
                "BASIC_ATTACK_TARGET entityId={} accepted={} reject={}",
                target.entityId(), target.accepted(), target.rejectReason()));
    }

    public static void logInfrastructureFailure(BasicAttackSource source, BasicAttackFailureReason reason,
            RuntimeException exception) {
        LOGGER.warn("BASIC_ATTACK_INFRA_FAILURE source={} reason={}", source, reason, exception);
    }

    public static void logDamageApplyFailure(ServerPlayer attacker, LivingEntity target, CombatResolution resolution) {
        LOGGER.debug("DAMAGE_APPLY_FAILED attacker={} target={} type={} amount={} note=TARGET_DAMAGE_REJECTED_after_guard_mark",
                attacker != null && attacker.getGameProfile() != null ? attacker.getGameProfile().getName() : "unknown",
                target != null ? target.getId() : -1,
                resolution != null ? resolution.resultType() : "unknown",
                resolution != null ? resolution.finalAmount() : 0.0D);
    }

    private static String actorName(CombatRequestContext ctx) {
        if (ctx == null || ctx.actor() == null || ctx.actor().getGameProfile() == null) {
            return "unknown";
        }
        String name = ctx.actor().getGameProfile().getName();
        return (name == null || name.isBlank()) ? "unknown" : name;
    }
}
