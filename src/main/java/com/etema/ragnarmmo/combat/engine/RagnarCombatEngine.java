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
import com.etema.ragnarmmo.skills.runtime.SkillManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

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

    private RagnarCombatEngine() {
    }

    public static RagnarCombatEngine get() {
        return INSTANCE;
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
        CombatRejectReason reject = validationService.validateBasicAttack(ctx, state, now, cooldownService);
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

    public BasicAttackOutcome handleBasicAttackRequest(CombatRequestContext ctx, BasicAttackSource source,
            List<ResolvedTargetCandidate> targetResults) {
        CombatDebugLog.logAttackRequest(ctx);
        ServerPlayer attacker = ctx.actor();
        long nowTick = attacker.serverLevel().getGameTime();
        CombatActorState actorState = state(attacker);

        actorState.setLastAcceptedSequenceId(ctx.sequenceId());

        int cooldownTicks = AttackCadenceCalculator.computeIntervalTicks(attacker, ctx.offHand());
        cooldownService.markBasicAttackUsed(actorState.getCooldowns(), nowTick, cooldownTicks);

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

            List<CombatResolution> results = new ArrayList<>();
            BasicAttackFailureReason applyFailure = BasicAttackFailureReason.NONE;
            for (CombatTargetCandidate candidate : ctx.candidates()) {
                CombatResolution resolution = resolveSingleBasicHit(attacker, attackProfile, candidate,
                        attackerStats.getLevel());
                results.add(resolution);

                BasicAttackFailureReason failure = applyResolution(attacker, resolution);
                if (failure != BasicAttackFailureReason.NONE && applyFailure == BasicAttackFailureReason.NONE) {
                    applyFailure = failure;
                }

                CombatDebugLog.logHitResolution(resolution);
            }

            if (results.isEmpty()) {
                return infrastructureFailure(source, targetResults, BasicAttackFailureReason.NO_RESOLUTION_PRODUCED);
            }

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

    private CombatResolution resolveSingleBasicHit(ServerPlayer attacker, HandAttackProfile attackProfile,
                                                   CombatTargetCandidate candidate, int attackerLevel) {
        net.minecraft.world.entity.Entity targetEntity = attacker.serverLevel().getEntity(candidate.entityId());
        if (!(targetEntity instanceof LivingEntity target)) {
            return CombatResolution.miss(attacker.getId(), candidate.entityId());
        }

        CombatantProfile attackerProfile = CombatantProfileResolver.resolvePlayer(attacker, attackProfile).orElse(null);
        CombatantProfile defenderProfile = resolveTargetProfile(target);
        var result = combatContract.resolveBasicAttack(
                attackerProfile,
                defenderProfile,
                new ActionIntent.BasicAttackIntent(attackProfile.offHand()),
                deterministicRandom(attacker, target, attackerLevel));
        if (result.rejected() || result.resolution() == null) {
            CombatDebugLog.logValidationReject(null, "COMBAT_CONTRACT_REJECTED_" + result.rejectReason());
            return CombatResolution.miss(attacker.getId(), target.getId());
        }
        return result.resolution();
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
            applyResolution(ctx.actor(), res);
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
        net.minecraft.world.entity.Entity target = attacker.serverLevel().getEntity(resolution.targetId());
        if (!(target instanceof LivingEntity livingTarget)) {
            return BasicAttackFailureReason.TARGET_DAMAGE_REJECTED;
        }

        if (resolution.resultType() == CombatHitResultType.HIT || resolution.resultType() == CombatHitResultType.CRIT) {
            float damage = (float) resolution.finalAmount();

            // Mark as processed to avoid CommonEvents double-calc
            DamageProcessingGuard.markProcessedPlayer(livingTarget);

            boolean applied = SkillDamageHelper.dealSkillDamage(livingTarget, attacker.damageSources().playerAttack(attacker), damage);
            if (!applied) {
                CombatDebugLog.logDamageApplyFailure(attacker, livingTarget, resolution);
                feedbackService.sendBasicAttackFeedback(attacker, livingTarget, resolution);
                return BasicAttackFailureReason.TARGET_DAMAGE_REJECTED;
            }
        }

        feedbackService.sendBasicAttackFeedback(attacker, livingTarget, resolution);
        return BasicAttackFailureReason.NONE;
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
                DamageProcessingGuard.markProcessedPlayer(p);
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
