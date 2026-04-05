package com.etema.ragnarmmo.combat.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative combat entry point.
 */
public class RagnarCombatEngine {
    private static final RagnarCombatEngine INSTANCE = new RagnarCombatEngine();

    private final RagnarCombatCooldownService cooldownService = new RagnarCombatCooldownService();
    private final RagnarCombatValidationService validationService = new RagnarCombatValidationService();
    private final RagnarHitCalculator hitCalculator = new RagnarHitCalculator();
    private final RagnarDamageCalculator damageCalculator = new RagnarDamageCalculator();
    private final Map<UUID, CombatActorState> actorStates = new ConcurrentHashMap<>();

    private RagnarCombatEngine() {
    }

    public static RagnarCombatEngine get() {
        return INSTANCE;
    }

    public CombatActorState state(ServerPlayer player) {
        return actorStates.computeIfAbsent(player.getUUID(), ignored -> new CombatActorState());
    }

    public List<CombatResolution> handleBasicAttackRequest(CombatRequestContext ctx) {
        CombatDebugLog.logAttackRequest(ctx);
        long nowTick = ctx.actor().serverLevel().getGameTime();
        CombatActorState actorState = state(ctx.actor());

        String reject = validationService.validateBasicAttack(ctx, actorState, nowTick, cooldownService);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, reject);
            return Collections.emptyList();
        }

        actorState.setLastAcceptedSequenceId(ctx.sequenceId());
        cooldownService.markBasicAttackUsed(actorState.getCooldowns(), nowTick, 10);

        List<CombatResolution> results = new ArrayList<>();
        for (var candidate : ctx.candidates()) {
            CombatHitResultType hitResult = hitCalculator.rollBasicAttackHit(100.0D, 0.0D);
            CombatResolution resolution = (hitResult == CombatHitResultType.MISS)
                    ? CombatResolution.miss(ctx.actor().getId(), candidate.entityId())
                    : CombatResolution.hit(ctx.actor().getId(), candidate.entityId(), 1.0D,
                            damageCalculator.computePhysicalDamage(1.0D, 1.0D, 0.0D),
                            hitCalculator.rollCritical(0.0D));
            results.add(resolution);
            CombatDebugLog.logHitResolution(resolution);
        }
        return results;
    }

    public List<CombatResolution> handleSkillUseRequest(CombatRequestContext ctx) {
        CombatDebugLog.logSkillRequest(ctx);
        long nowTick = ctx.actor().serverLevel().getGameTime();
        CombatActorState actorState = state(ctx.actor());

        String reject = validationService.validateSkillRequest(ctx, actorState, nowTick, cooldownService);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, reject);
            return Collections.emptyList();
        }

        actorState.setLastAcceptedSequenceId(ctx.sequenceId());
        cooldownService.markSkillUsed(actorState.getCooldowns(), ctx.skillId(), nowTick, 20);
        return Collections.emptyList();
    }
}
