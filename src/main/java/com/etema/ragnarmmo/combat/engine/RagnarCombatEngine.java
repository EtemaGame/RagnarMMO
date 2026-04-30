package com.etema.ragnarmmo.combat.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.etema.ragnarmmo.combat.api.BasicAttackFailureReason;
import com.etema.ragnarmmo.combat.api.BasicAttackOutcome;
import com.etema.ragnarmmo.combat.api.BasicAttackSource;
import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatRejectReason;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.api.CombatTargetCandidate;
import com.etema.ragnarmmo.combat.api.RagnarAttackRequest;
import com.etema.ragnarmmo.combat.api.ResolvedTargetCandidate;
import com.etema.ragnarmmo.combat.contract.ActionIntent;
import com.etema.ragnarmmo.combat.contract.CombatContract;
import com.etema.ragnarmmo.combat.contract.CombatStrictMode;
import com.etema.ragnarmmo.combat.contract.CombatantProfile;
import com.etema.ragnarmmo.combat.contract.CombatantProfileResolver;
import com.etema.ragnarmmo.combat.credit.RoKillCreditService;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.combat.profile.HandAttackProfile;
import com.etema.ragnarmmo.combat.profile.HandAttackProfileResolver;
import com.etema.ragnarmmo.combat.timing.AttackCadenceCalculator;
import com.etema.ragnarmmo.combat.timing.CombatTimingCalculator;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.state.CombatCastState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;
import com.etema.ragnarmmo.combat.targeting.RagnarTargetResolver;
import com.etema.ragnarmmo.combat.targeting.ServerAuthoritativeTargetResolver;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.player.RoPlayerDataAccess;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skills.runtime.SkillManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

/**
 * Server-authoritative combat entry point.
 */
public class RagnarCombatEngine {
    private static final RagnarCombatEngine INSTANCE = new RagnarCombatEngine();

    private final RagnarCombatCooldownService cooldownService = new RagnarCombatCooldownService();
    private final RagnarCombatValidationService validationService = new RagnarCombatValidationService();
    private final RagnarHitCalculator hitCalculator = new RagnarHitCalculator();
    private final RagnarDamageCalculator damageCalculator = new RagnarDamageCalculator();
    private final CombatContract combatContract = new CombatContract(hitCalculator, damageCalculator);
    private final RagnarSkillResolver skillResolver = new RagnarSkillResolver(hitCalculator, damageCalculator, combatContract);
    private final RagnarCombatFeedbackService feedbackService = new RagnarCombatFeedbackService();
    private final RagnarTargetResolver targetResolver = new ServerAuthoritativeTargetResolver();
    private final Map<UUID, CombatActorState> actorStates = new ConcurrentHashMap<>();
    private static final ResourceLocation AUTO_COUNTER =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "auto_counter");
    private static final ResourceLocation BLITZ_BEAT =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "blitz_beat");
    private static final ResourceLocation STEEL_CROW =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "steel_crow");

    private RagnarCombatEngine() {
    }

    public static RagnarCombatEngine get() {
        return INSTANCE;
    }

    public CombatContract contract() {
        return combatContract;
    }

    public CombatActorState state(ServerPlayer player) {
        return actorStates.computeIfAbsent(player.getUUID(), ignored -> new CombatActorState());
    }

    public BasicAttackOutcome processBasicAttackRequest(ServerPlayer player, RagnarAttackRequest request) {
        return processBasicAttackRequest(player, request, BasicAttackSource.SERVER_ATTACK_EVENT);
    }

    public BasicAttackOutcome processBasicAttackRequest(ServerPlayer player, RagnarAttackRequest request,
            BasicAttackSource source) {
        CombatActorState state = state(player);
        long now = player.serverLevel().getGameTime();
        if (source == BasicAttackSource.CLIENT_PACKET && request != null && !request.candidates().isEmpty()) {
            state.setLastObservedPacketIntent(new CombatActorState.RecentBasicAttackIntent(
                    now,
                    request.candidates().get(0).entityId(),
                    request.sequenceId()));
            CombatDebugLog.logBasicAttackDedupe("packet_observed", source, player, request.sequenceId(),
                    request.candidates().get(0).entityId(), now);
        }

        // 1. Initial Authoritative Target Resolution
        List<ResolvedTargetCandidate> targetResults = targetResolver.resolveCandidates(player, request.candidates());

        // 2. Normalized Request Context
        List<CombatTargetCandidate> candidates = targetResults.stream()
                .filter(ResolvedTargetCandidate::accepted)
                .map(t -> new CombatTargetCandidate(t.entityId(), "domain", 0, false))
                .toList();

        CombatRequestContext ctx = new CombatRequestContext(
                player,
                com.etema.ragnarmmo.combat.api.CombatActionType.BASIC_ATTACK,
                request.sequenceId(),
                request.comboIndex(),
                request.offHand(),
                request.selectedSlot(),
                null,
                candidates);

        // 3. Complete Server Validation
        CombatRejectReason reject = validationService.validateBasicAttack(ctx, state, now, cooldownService, source);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, "REJECTED_" + reject.name());
            BasicAttackOutcome outcome = BasicAttackOutcome.rejected(source, reject, true, targetResults);
            CombatDebugLog.logBasicAttackOutcome(outcome);
            return outcome;
        }

        BasicAttackOutcome outcome = handleBasicAttackRequest(ctx, source, targetResults);
        CombatDebugLog.logBasicAttackOutcome(outcome);
        return outcome;
    }

    public boolean hasRecentClientPacketAttack(ServerPlayer player, int targetId) {
        if (player == null) {
            return false;
        }
        CombatActorState actorState = state(player);
        long now = player.serverLevel().getGameTime();
        boolean recent = actorState.getLastObservedPacketIntent().matchesRecent(now, targetId);
        if (recent) {
            CombatDebugLog.logBasicAttackDedupe("server_event_ignored_recent_packet", BasicAttackSource.SERVER_ATTACK_EVENT,
                    player, actorState.getLastObservedPacketIntent().sequenceId(), targetId, now);
        }
        return recent;
    }

    public BasicAttackOutcome handleBasicAttackRequest(CombatRequestContext ctx, BasicAttackSource source,
            List<ResolvedTargetCandidate> targetResults) {
        CombatDebugLog.logAttackRequest(ctx);
        ServerPlayer attacker = ctx.actor();
        long nowTick = attacker.serverLevel().getGameTime();
        CombatActorState actorState = state(attacker);

        int cooldownTicks = AttackCadenceCalculator.computeIntervalTicks(attacker, ctx.offHand());

        try {
            // Attacker Data (Authoritative)
            var attackerStats = RagnarCoreAPI.get(attacker).orElse(null);
            if (attackerStats == null) {
                return infrastructureFailure(source, targetResults, BasicAttackFailureReason.MISSING_ATTACKER_STATS);
            }

            HandAttackProfile attackProfile = HandAttackProfileResolver.resolve(attacker, ctx.offHand()).orElse(null);
            if (attackProfile == null) {
                return infrastructureFailure(source, targetResults, BasicAttackFailureReason.MISSING_ATTACK_PROFILE);
            }

            CombatDebugLog.logAttackPacing(ctx, AttackHandResolver.isDualWielding(attacker),
                    attackProfile.aps(), cooldownTicks);

            CombatantProfile attackerProfile = CombatantProfileResolver.resolvePlayer(attacker, attackProfile).orElse(null);
            if (attackerProfile == null) {
                return infrastructureFailure(source, targetResults, BasicAttackFailureReason.MISSING_ATTACKER_STATS);
            }

            List<CombatResolution> results = new ArrayList<>();
            BasicHitPreparation.InfrastructureFailure firstInfrastructureFailure = null;
            BasicAttackFailureReason applyFailure = BasicAttackFailureReason.NONE;
            for (CombatTargetCandidate candidate : ctx.candidates()) {
                BasicHitPreparation preparation = prepareSingleBasicHit(attacker, attackerProfile, attackProfile,
                        candidate, attackerStats.getLevel());
                if (preparation instanceof BasicHitPreparation.SkippedInvalidTarget skipped) {
                    CombatDebugLog.logBasicAttackPreparation(ctx, source, skipped.reason(), skipped.targetId(), null);
                    continue;
                }
                if (preparation instanceof BasicHitPreparation.InfrastructureFailure failure) {
                    CombatDebugLog.logBasicAttackPreparation(ctx, source, failure.failureReason(), candidate.entityId(),
                            failure.contractRejectReason());
                    if (firstInfrastructureFailure == null) {
                        firstInfrastructureFailure = failure;
                    }
                    continue;
                }

                CombatResolution resolution = ((BasicHitPreparation.Resolved) preparation).resolution();
                results.add(resolution);

                BasicAttackFailureReason failure = applyResolution(attacker, resolution, false);
                if (failure != BasicAttackFailureReason.NONE && applyFailure == BasicAttackFailureReason.NONE) {
                    applyFailure = failure;
                }
                if (failure == BasicAttackFailureReason.NONE) {
                    triggerSecondaryActionsAfterPlayerBasicAttack(attacker, resolution);
                }

                CombatDebugLog.logHitResolution(resolution);
            }

            if (results.isEmpty()) {
                BasicAttackFailureReason reason = firstInfrastructureFailure != null
                        ? firstInfrastructureFailure.failureReason()
                        : BasicAttackFailureReason.NO_RESOLUTION_PRODUCED;
                return infrastructureFailure(source, targetResults, reason);
            }

            commitAcceptedBasicAttack(actorState, source, ctx.sequenceId(), nowTick, cooldownTicks, attacker,
                    results.get(0).targetId());
            BasicAttackOutcome outcome = BasicAttackOutcome.resolved(source, results, targetResults, false);
            return applyFailure == BasicAttackFailureReason.NONE ? outcome : outcome.withFailure(applyFailure);
        } catch (RuntimeException ex) {
            CombatDebugLog.logInfrastructureFailure(source, BasicAttackFailureReason.INTERNAL_ERROR, ex);
            if (CombatStrictMode.current() == CombatStrictMode.DEV) {
                throw ex;
            }
            return infrastructureFailure(source, targetResults, BasicAttackFailureReason.INTERNAL_ERROR);
        }
    }

    private BasicHitPreparation prepareSingleBasicHit(ServerPlayer attacker, CombatantProfile attackerProfile,
                                                   HandAttackProfile attackProfile, CombatTargetCandidate candidate,
                                                   int attackerLevel) {
        net.minecraft.world.entity.Entity targetEntity = attacker.serverLevel().getEntity(candidate.entityId());
        if (!(targetEntity instanceof LivingEntity target)) {
            return new BasicHitPreparation.SkippedInvalidTarget(candidate.entityId(), BasicAttackFailureReason.TARGET_NOT_FOUND);
        }

        CombatantProfile defenderProfile = resolveTargetProfile(target);
        if (defenderProfile == null) {
            return new BasicHitPreparation.InfrastructureFailure(BasicAttackFailureReason.MISSING_TARGET_PROFILE,
                    "missing_target_profile");
        }
        var result = combatContract.resolveBasicAttack(
                attackerProfile,
                defenderProfile,
                new ActionIntent.BasicAttackIntent(attackProfile.offHand()),
                deterministicRandom(attacker, target, attackerLevel));
        if (result.rejected() || result.resolution() == null) {
            CombatDebugLog.logValidationReject(null, "COMBAT_CONTRACT_REJECTED_" + result.rejectReason());
            return new BasicHitPreparation.InfrastructureFailure(BasicAttackFailureReason.CONTRACT_REJECTED,
                    result.rejectReason());
        }
        return new BasicHitPreparation.Resolved(result.resolution());
    }

    private void commitAcceptedBasicAttack(CombatActorState actorState, BasicAttackSource source, int sequenceId,
            long nowTick, int cooldownTicks, ServerPlayer actor, int targetId) {
        cooldownService.markBasicAttackUsed(actorState.getCooldowns(), nowTick, cooldownTicks);
        if (source == BasicAttackSource.SERVER_ATTACK_EVENT) {
            actorState.setLastServerFallbackTick(nowTick);
            CombatDebugLog.logBasicAttackDedupe("fallback_accepted", source, actor, sequenceId, targetId, nowTick);
            return;
        }
        actorState.setLastClientPacketSequenceId(sequenceId);
        actorState.setLastAcceptedSequenceId(sequenceId);
        CombatDebugLog.logBasicAttackDedupe("packet_accepted", source, actor, sequenceId, targetId, nowTick);
    }

    private BasicAttackOutcome infrastructureFailure(BasicAttackSource source,
            List<ResolvedTargetCandidate> targetResults,
            BasicAttackFailureReason reason) {
        return new BasicAttackOutcome(source, false, true, null, reason, List.of(), targetResults, false);
    }

    private CombatantProfile resolveTargetProfile(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            return CombatantProfileResolver.resolvePlayer(player, null).orElse(null);
        }
        if (target instanceof net.minecraft.world.entity.Mob mob) {
            return CombatantProfileResolver.resolveMob(mob, CombatStrictMode.current()).orElse(null);
        }
        return null;
    }

    private static java.util.Random deterministicRandom(ServerPlayer attacker, LivingEntity target, int sequenceSalt) {
        long seed = 31L * attacker.serverLevel().getGameTime()
                + 17L * attacker.getId()
                + 13L * target.getId()
                + sequenceSalt;
        return new java.util.Random(seed);
    }

    private sealed interface BasicHitPreparation {
        record Resolved(CombatResolution resolution) implements BasicHitPreparation {
        }

        record SkippedInvalidTarget(int targetId, BasicAttackFailureReason reason) implements BasicHitPreparation {
        }

        record InfrastructureFailure(BasicAttackFailureReason failureReason,
                                     String contractRejectReason) implements BasicHitPreparation {
        }
    }

    public List<CombatResolution> handleSkillUseRequest(CombatRequestContext ctx) {
        CombatDebugLog.logSkillRequest(ctx);
        long nowTick = ctx.actor().serverLevel().getGameTime();
        CombatActorState actorState = state(ctx.actor());

        CombatRejectReason reject = validationService.validateSkillRequest(ctx, actorState, nowTick, cooldownService);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, "REJECTED_" + reject.name());
            return Collections.emptyList();
        }

        actorState.setLastAcceptedSequenceId(ctx.sequenceId());

        SkillDefinition definition = resolveSkillDefinition(ctx.skillId());
        int skillLevel = resolveRequestedSkillLevel(ctx);
        if (definition != null) {
            if (!validateSkillAccessAndResource(ctx, definition, skillLevel, false)) {
                return Collections.emptyList();
            }
            var timing = CombatTimingCalculator.resolveSkillTiming(ctx.actor(), definition, skillLevel);
            if (timing.totalCastTicks() > 0) {
                beginCast(ctx, actorState, nowTick, skillLevel, timing);
                return Collections.emptyList();
            }

            if (!validateSkillAccessAndResource(ctx, definition, skillLevel, true)) {
                return Collections.emptyList();
            }
            List<CombatResolution> resolutions = resolveAndApplySkill(ctx, actorState, nowTick);
            applySkillTiming(actorState, ctx.skillId(), nowTick, timing);
            return resolutions;
        }

        List<CombatResolution> resolutions = resolveAndApplySkill(ctx, actorState, nowTick);
        cooldownService.markSkillUsed(actorState.getCooldowns(), ctx.skillId(), nowTick, 20);

        return resolutions;
    }

    private List<CombatResolution> resolveAndApplySkill(CombatRequestContext ctx, CombatActorState actorState, long nowTick) {
        List<CombatResolution> resolutions = skillResolver.resolveSkill(ctx, actorState, nowTick);
        for (CombatResolution res : resolutions) {
            applyResolution(ctx.actor(), res, true);
        }
        return resolutions;
    }

    private void beginCast(CombatRequestContext ctx, CombatActorState actorState, long nowTick, int skillLevel,
            CombatTimingCalculator.TimingProfile timing) {
        Integer targetEntityId = ctx.candidates().isEmpty() ? null : ctx.candidates().get(0).entityId();
        actorState.getCastState().start(
                ctx.skillId(),
                skillLevel,
                nowTick,
                timing.variableCastTicks(),
                timing.fixedCastTicks(),
                timing.afterCastDelayTicks(),
                timing.globalDelayTicks(),
                timing.cooldownTicks(),
                targetEntityId,
                null,
                ctx.selectedSlot(),
                ctx.offHand());
        Network.sendToPlayer(ctx.actor(), new com.etema.ragnarmmo.combat.net.ClientboundRagnarCastStatePacket(
                ctx.actor().getId(),
                ctx.skillId(),
                com.etema.ragnarmmo.combat.net.ClientboundRagnarCastStatePacket.CastState.STARTED,
                timing.totalCastTicks()));
    }

    private void applySkillTiming(CombatActorState actorState, String skillId, long nowTick,
            CombatTimingCalculator.TimingProfile timing) {
        cooldownService.markSkillUsed(actorState.getCooldowns(), skillId, nowTick, timing.cooldownTicks());
        cooldownService.applyAfterCastDelay(actorState.getCooldowns(), nowTick, timing.afterCastDelayTicks());
        cooldownService.applyGlobalDelay(actorState.getCooldowns(), nowTick, timing.globalDelayTicks());
    }

    public void tickActor(ServerPlayer player) {
        CombatActorState actorState = state(player);
        CombatCastState cast = actorState.getCastState();
        long nowTick = player.serverLevel().getGameTime();
        if (cast.getActiveSkillId() == null || cast.isCasting(nowTick)) {
            return;
        }

        String skillId = cast.getActiveSkillId();
        int skillLevel = cast.getActiveSkillLevel();
        java.util.List<CombatTargetCandidate> candidates = cast.getTargetEntityId() == null
                ? java.util.List.of()
                : java.util.List.of(new CombatTargetCandidate(cast.getTargetEntityId(), "cast", 0.0D, false));
        CombatRequestContext completionCtx = new CombatRequestContext(
                player,
                com.etema.ragnarmmo.combat.api.CombatActionType.SKILL,
                actorState.getLastAcceptedSequenceId(),
                0,
                cast.isOffHand(),
                cast.getSelectedSlot(),
                skillId,
                candidates,
                java.util.Map.of("level", skillLevel));
        SkillDefinition definition = resolveSkillDefinition(skillId);
        if (definition != null && !validateSkillAccessAndResource(completionCtx, definition, skillLevel, true)) {
            cast.clear();
            return;
        }
        resolveAndApplySkill(completionCtx, actorState, nowTick);
        cooldownService.markSkillUsed(actorState.getCooldowns(), skillId, nowTick, cast.getCooldownTicks());
        cooldownService.applyAfterCastDelay(actorState.getCooldowns(), nowTick, cast.getAfterCastDelayTicks());
        cooldownService.applyGlobalDelay(actorState.getCooldowns(), nowTick, cast.getGlobalDelayTicks());
        Network.sendToPlayer(player, new com.etema.ragnarmmo.combat.net.ClientboundRagnarCastStatePacket(
                player.getId(),
                skillId,
                com.etema.ragnarmmo.combat.net.ClientboundRagnarCastStatePacket.CastState.COMPLETED,
                cast.getTotalCastTicks()));
        cast.clear();
    }

    private static SkillDefinition resolveSkillDefinition(String rawSkillId) {
        if (rawSkillId == null || rawSkillId.isBlank()) {
            return null;
        }
        ResourceLocation id = rawSkillId.contains(":")
                ? ResourceLocation.tryParse(rawSkillId)
                : ResourceLocation.fromNamespaceAndPath(com.etema.ragnarmmo.RagnarMMO.MODID, rawSkillId);
        return id == null ? null : SkillRegistry.get(id).orElse(null);
    }

    private static int resolveRequestedSkillLevel(CombatRequestContext ctx) {
        Object level = ctx.metadata().get("level");
        return level instanceof Integer value ? Math.max(1, value) : 1;
    }

    private static boolean validateSkillAccessAndResource(CombatRequestContext ctx, SkillDefinition definition,
            int skillLevel, boolean consumeResource) {
        return RoPlayerDataAccess.get(ctx.actor()).map(data -> {
            SkillManager skills = (SkillManager) data.getSkills();
            ResourceLocation id = definition.getId();
            int learnedLevel = skills.getSkillLevel(id);
            if (skillLevel <= 0 || skillLevel > definition.getMaxLevel() || learnedLevel < skillLevel) {
                CombatDebugLog.logValidationReject(ctx, "SKILL_LEVEL_NOT_LEARNED");
                return false;
            }

            if (!definition.getAllowedJobs().isEmpty()) {
                String currentJobId = data.getStats().getJobId();
                com.etema.ragnarmmo.common.api.jobs.JobType jobType =
                        com.etema.ragnarmmo.common.api.jobs.JobType.fromId(currentJobId);
                boolean jobAllowed = definition.getAllowedJobs().stream()
                        .anyMatch(jobType::matchesSkillRule)
                        || definition.getTier() == com.etema.ragnarmmo.skills.api.SkillTier.LIFE;
                if (!jobAllowed) {
                    CombatDebugLog.logValidationReject(ctx, "SKILL_WRONG_JOB");
                    return false;
                }
            }

            com.etema.ragnarmmo.skills.api.ResourceType resourceType = definition.getResourceType();
            int cost = Math.max(0, definition.getResourceCost(skillLevel));
            if (resourceType != com.etema.ragnarmmo.skills.api.ResourceType.COOLDOWN_ONLY
                    && data.getStats().getCurrentResource() < cost) {
                CombatDebugLog.logValidationReject(ctx, "SKILL_INSUFFICIENT_RESOURCE");
                return false;
            }
            if (consumeResource && resourceType != com.etema.ragnarmmo.skills.api.ResourceType.COOLDOWN_ONLY && cost > 0) {
                if (!data.getStats().consumeResource(cost)) {
                    CombatDebugLog.logValidationReject(ctx, "SKILL_RESOURCE_CONSUME_FAILED");
                    return false;
                }
                com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService.sync(
                        ctx.actor(),
                        data.getStats(),
                        com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.allMask());
                Network.sendToPlayer(ctx.actor(),
                        new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(skills.serializeNBT()));
            }
            return true;
        }).orElse(false);
    }

    private BasicAttackFailureReason applyResolution(ServerPlayer attacker, CombatResolution resolution) {
        return applyResolution(attacker, resolution, false);
    }

    private BasicAttackFailureReason applyResolution(ServerPlayer attacker, CombatResolution resolution, boolean skillDamage) {
        net.minecraft.world.entity.Entity target = attacker.serverLevel().getEntity(resolution.targetId());
        if (!(target instanceof LivingEntity livingTarget)) {
            return BasicAttackFailureReason.TARGET_DAMAGE_REJECTED;
        }

        if (resolution.resultType() == CombatHitResultType.HIT || resolution.resultType() == CombatHitResultType.CRIT) {
            float damage = (float) resolution.finalAmount();

            // Mark as processed to avoid CommonEvents double-calc
            if (skillDamage) {
                DamageProcessingGuard.markSkillContractDamage(livingTarget);
            } else {
                DamageProcessingGuard.markBasicAttack(livingTarget);
            }

            RoKillCreditService.recordPlayerContribution(attacker, livingTarget, resolution);
            boolean applied = SkillDamageHelper.dealSkillDamage(livingTarget, attacker.damageSources().playerAttack(attacker), damage);
            if (!applied) {
                RoKillCreditService.clearPlayerContribution(attacker, livingTarget);
                CombatDebugLog.logDamageApplyFailure(attacker, livingTarget, resolution);
                feedbackService.sendBasicAttackFeedback(attacker, livingTarget, resolution);
                return BasicAttackFailureReason.TARGET_DAMAGE_REJECTED;
            }
        }

        feedbackService.sendBasicAttackFeedback(attacker, livingTarget, resolution);
        return BasicAttackFailureReason.NONE;
    }

    public void triggerAutoBlitzFromRangedAttack(ServerPlayer attacker, LivingEntity target, CombatResolution trigger) {
        if (attacker == null || target == null || trigger == null || !isDamagingOutcome(trigger)) {
            return;
        }
        triggerAutoBlitz(attacker, target, trigger);
    }

    private void triggerSecondaryActionsAfterPlayerBasicAttack(ServerPlayer attacker, CombatResolution trigger) {
        if (attacker == null || trigger == null || !isDamagingOutcome(trigger)) {
            return;
        }
        net.minecraft.world.entity.Entity targetEntity = attacker.serverLevel().getEntity(trigger.targetId());
        if (targetEntity instanceof LivingEntity target) {
            triggerAutoBlitz(attacker, target, trigger);
        }
    }

    private void triggerAutoCounter(ServerPlayer defender, LivingEntity attacker, CombatResolution incoming) {
        if (defender == null || attacker == null || incoming == null || !isDamagingOutcome(incoming)) {
            return;
        }
        if (!attacker.isAlive() || attacker.distanceToSqr(defender) > 16.0D) {
            return;
        }

        int level = skillLevel(defender, AUTO_COUNTER);
        if (level <= 0) {
            return;
        }

        java.util.Random rng = deterministicSecondaryRandom(defender, attacker, incoming, 7100 + level);
        double chance = Math.min(1.0D, level * 0.20D);
        if (rng.nextDouble() >= chance) {
            return;
        }

        HandAttackProfile attackProfile = HandAttackProfileResolver.resolve(defender, false).orElse(null);
        CombatantProfile attackerProfile = CombatantProfileResolver.resolvePlayer(defender, attackProfile).orElse(null);
        CombatantProfile defenderProfile = resolveTargetProfile(attacker);
        var result = combatContract.resolveBasicAttack(
                attackerProfile,
                defenderProfile,
                new ActionIntent.BasicAttackIntent(false),
                rng);
        if (result.rejected() || result.resolution() == null || !isDamagingOutcome(result.resolution())) {
            return;
        }

        CombatResolution base = result.resolution();
        CombatResolution counter = CombatResolution.hit(
                defender.getId(),
                attacker.getId(),
                base.baseAmount(),
                Math.max(1.0D, base.finalAmount() * 1.5D),
                true);
        applySecondarySkillResolution(defender, counter);
    }

    private void triggerAutoBlitz(ServerPlayer attacker, LivingEntity target, CombatResolution trigger) {
        if (attacker == null || target == null || trigger == null || !isDamagingOutcome(trigger)) {
            return;
        }
        if (!hasFalcon(attacker) || !isRangedWeapon(attacker)) {
            return;
        }

        int level = skillLevel(attacker, BLITZ_BEAT);
        if (level <= 0) {
            return;
        }

        java.util.Random rng = deterministicSecondaryRandom(attacker, target, trigger, 9100 + level);
        double luk = com.etema.ragnarmmo.common.api.stats.StatAttributes.getTotal(
                attacker,
                com.etema.ragnarmmo.common.api.stats.StatKeys.LUK);
        double chance = Math.min(1.0D, ((luk * 0.3D) + 1.0D) / 100.0D);
        if (rng.nextDouble() >= chance) {
            return;
        }

        double dex = com.etema.ragnarmmo.common.api.stats.StatAttributes.getTotal(
                attacker,
                com.etema.ragnarmmo.common.api.stats.StatKeys.DEX);
        double intel = com.etema.ragnarmmo.common.api.stats.StatAttributes.getTotal(
                attacker,
                com.etema.ragnarmmo.common.api.stats.StatKeys.INT);
        int steelCrow = skillLevel(attacker, STEEL_CROW);
        int hits = Math.max(1, 1 + ((level - 1) / 2));
        double perHit = 80.0D + (6.0D * steelCrow) + (2.0D * ((dex / 10.0D) + intel));
        double totalDamage = Math.max(1.0D, perHit * hits);

        CombatResolution blitz = CombatResolution.hit(
                attacker.getId(),
                target.getId(),
                totalDamage,
                totalDamage,
                false);
        applySecondarySkillResolution(attacker, blitz);
    }

    private BasicAttackFailureReason applySecondarySkillResolution(ServerPlayer attacker, CombatResolution resolution) {
        if (attacker == null || resolution == null) {
            return BasicAttackFailureReason.NO_RESOLUTION_PRODUCED;
        }
        return applyResolution(attacker, resolution, true);
    }

    private static boolean isDamagingOutcome(CombatResolution resolution) {
        return resolution.resultType() == CombatHitResultType.HIT || resolution.resultType() == CombatHitResultType.CRIT;
    }

    private static int skillLevel(ServerPlayer player, ResourceLocation skillId) {
        return PlayerSkillsProvider.get(player)
                .map(skills -> skills.getSkillLevel(skillId))
                .orElse(0);
    }

    private static boolean hasFalcon(ServerPlayer player) {
        return player.getPersistentData().getBoolean("ragnar_has_falcon")
                || skillLevel(player, ResourceLocation.fromNamespaceAndPath("ragnarmmo", "falconry_mastery")) > 0;
    }

    private static boolean isRangedWeapon(ServerPlayer player) {
        var item = player.getMainHandItem().getItem();
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    private static java.util.Random deterministicSecondaryRandom(ServerPlayer actor, LivingEntity target,
            CombatResolution trigger, int salt) {
        long seed = 43L * actor.serverLevel().getGameTime()
                + 29L * actor.getId()
                + 17L * target.getId()
                + 7L * trigger.targetId()
                + salt;
        return new java.util.Random(seed);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "ragnarmmo")
    public static class CombatEventHandler {
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
        public static void onMobAttackPlayer(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
            if (!(e.getEntity() instanceof ServerPlayer p))
                return;
            if (p.level().isClientSide())
                return;

            if (DamageProcessingGuard.isProcessedPlayer(p))
                return;

            // Mob-to-player damage handling with Authority
            RagnarCoreAPI.get(p).ifPresent(stats -> {
                LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity living ? living : null;
                if (attacker == null || isEnvironmentalDamage(e.getSource())) {
                    return;
                }

                CombatantProfile attackerProfile = attacker instanceof net.minecraft.world.entity.Mob mob
                        ? CombatantProfileResolver.resolveMob(mob, CombatStrictMode.current()).orElse(null)
                        : attacker instanceof ServerPlayer sp
                                ? CombatantProfileResolver.resolvePlayer(sp, null).orElse(null)
                                : null;
                CombatantProfile defenderProfile = CombatantProfileResolver.resolvePlayer(p, null).orElse(null);
                var result = RagnarCombatEngine.get().combatContract.resolveBasicAttack(
                        attackerProfile,
                        defenderProfile,
                        new ActionIntent.BasicAttackIntent(false),
                        deterministicRandom(p, attacker, p.tickCount));
                if (result.rejected() || result.resolution() == null) {
                    e.setAmount(0);
                    e.setCanceled(true);
                    CombatDebugLog.logValidationReject(null, "MOB_TO_PLAYER_CONTRACT_REJECTED_" + result.rejectReason());
                    return;
                }

                CombatResolution resolution = result.resolution();
                if (resolution.resultType() == CombatHitResultType.MISS
                        || resolution.resultType() == CombatHitResultType.DODGE) {
                    e.setAmount(0);
                    e.setCanceled(true);
                    RagnarCombatEngine.get().feedbackService.sendBasicAttackFeedback(null, p, resolution);
                    return;
                }

                e.setAmount((float) resolution.finalAmount());
                DamageProcessingGuard.markMobToPlayerContract(p);
                RagnarCombatEngine.get().triggerAutoCounter(p, attacker, resolution);
            });
        }

        private static boolean isEnvironmentalDamage(net.minecraft.world.damagesource.DamageSource source) {
            return source.getEntity() == null;
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            if (!(event.player instanceof ServerPlayer player) || player.level().isClientSide()) {
                return;
            }
            RagnarCombatEngine.get().tickActor(player);
        }
    }
}
