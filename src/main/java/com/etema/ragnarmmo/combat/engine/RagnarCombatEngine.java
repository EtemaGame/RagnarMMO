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
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.combat.profile.HandAttackProfile;
import com.etema.ragnarmmo.combat.profile.HandAttackProfileResolver;
import com.etema.ragnarmmo.combat.timing.AttackCadenceCalculator;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;
import com.etema.ragnarmmo.combat.targeting.RagnarTargetResolver;
import com.etema.ragnarmmo.combat.targeting.ServerAuthoritativeTargetResolver;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
import com.etema.ragnarmmo.player.stats.compute.StatResolutionService;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative combat entry point.
 */
public class RagnarCombatEngine {
    private static final RagnarCombatEngine INSTANCE = new RagnarCombatEngine();

    private final RagnarCombatCooldownService cooldownService = new RagnarCombatCooldownService();
    private final RagnarCombatValidationService validationService = new RagnarCombatValidationService();
    private final RagnarHitCalculator hitCalculator = new RagnarHitCalculator();
    private final RagnarDamageCalculator damageCalculator = new RagnarDamageCalculator();
    private final RagnarSkillResolver skillResolver = new RagnarSkillResolver(hitCalculator, damageCalculator);
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
                return infrastructureFallback(source, targetResults, attacker, BasicAttackFailureReason.MISSING_ATTACKER_STATS);
            }

            HandAttackProfile attackProfile = HandAttackProfileResolver.resolve(attacker, ctx.offHand()).orElse(null);
            if (attackProfile == null) {
                return infrastructureFallback(source, targetResults, attacker, BasicAttackFailureReason.MISSING_ATTACK_PROFILE);
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
                return infrastructureFallback(source, targetResults, attacker, BasicAttackFailureReason.NO_RESOLUTION_PRODUCED);
            }

            BasicAttackOutcome outcome = BasicAttackOutcome.resolved(source, results, targetResults, false);
            return applyFailure == BasicAttackFailureReason.NONE ? outcome : outcome.withFailure(applyFailure);
        } catch (RuntimeException ex) {
            CombatDebugLog.logInfrastructureFailure(source, BasicAttackFailureReason.INTERNAL_ERROR, ex);
            return infrastructureFallback(source, targetResults, attacker, BasicAttackFailureReason.INTERNAL_ERROR);
        }
    }

    private CombatResolution resolveSingleBasicHit(ServerPlayer attacker, HandAttackProfile attackProfile,
                                                   CombatTargetCandidate candidate, int attackerLevel) {
        net.minecraft.world.entity.Entity targetEntity = attacker.serverLevel().getEntity(candidate.entityId());
        if (!(targetEntity instanceof LivingEntity target)) {
            return CombatResolution.miss(attacker.getId(), candidate.entityId());
        }

        // Attacker Data from authoritative source
        int lvl = Math.max(1, attackerLevel);
        ItemStack weapon = attackProfile.weapon();

        double totalBaseAtk = attackProfile.physicalAttack();
        double accuracy = attackProfile.accuracy();
        double critChance = attackProfile.critChance();

        // Defender Data
        DefenderStats def = fetchDefenderStats(target);

        // 1. Perfect Dodge check
        if (hitCalculator.rollPerfectDodge(def.perfectDodge(), attacker.getRandom())) {
            return CombatResolution.dodge(attacker.getId(), target.getId());
        }

        // 2. Hit/Crit check
        CombatHitResultType hitResult = hitCalculator.rollHitWithCrit(accuracy, def.flee(), critChance, def.critShield(),
                attacker.getRandom());

        if (hitResult == CombatHitResultType.MISS) {
            return CombatResolution.miss(attacker.getId(), target.getId());
        }

        // 3. Damage calculation
        int dex = (int) StatAttributes.getTotal(attacker, StatKeys.DEX);
        int luk = (int) StatAttributes.getTotal(attacker, StatKeys.LUK);

        double dmg = damageCalculator.computePhysicalDamage(totalBaseAtk, dex, luk,
                new java.util.Random(attacker.getRandom().nextLong()));

        if (hitResult == CombatHitResultType.CRIT) {
            dmg *= Math.max(1.0D, attackProfile.critDamageMultiplier());
        }

        // SIZE & ELEMENTAL
        var attackElement = com.etema.ragnarmmo.combat.element.CombatPropertyResolver.getOffensiveElement(attacker);
        dmg = damageCalculator.applyModifiers(dmg, weapon, target, attackElement, false);

        // DEFENSE
        dmg = damageCalculator.applyPhysicalDefense(dmg, def.vit(), def.agi(), lvl, def.armorEff());

        double finalDamage = Math.max(1.0D, dmg);

        return CombatResolution.hit(
                attacker.getId(),
                target.getId(),
                dmg,
                finalDamage,
                hitResult == CombatHitResultType.CRIT);
    }

    private BasicAttackOutcome infrastructureFallback(BasicAttackSource source,
            List<ResolvedTargetCandidate> targetResults,
            ServerPlayer attacker,
            BasicAttackFailureReason reason) {
        LivingEntity target = firstValidTarget(targetResults);
        CombatResolution fallback = target == null ? null : createMinimumFallbackHit(attacker, target);
        if (fallback != null) {
            CombatDebugLog.logHitResolution(fallback);
            BasicAttackFailureReason applyFailure = applyResolution(attacker, fallback);
            BasicAttackOutcome outcome = BasicAttackOutcome.infrastructureFallback(source, reason, fallback, targetResults);
            return applyFailure == BasicAttackFailureReason.NONE ? outcome : outcome.withFailure(applyFailure);
        }
        return new BasicAttackOutcome(source, true, true, null, reason, List.of(), targetResults, false);
    }

    private static LivingEntity firstValidTarget(List<ResolvedTargetCandidate> targetResults) {
        if (targetResults == null) {
            return null;
        }
        return targetResults.stream()
                .filter(ResolvedTargetCandidate::accepted)
                .map(ResolvedTargetCandidate::target)
                .findFirst()
                .orElse(null);
    }

    private static CombatResolution createMinimumFallbackHit(ServerPlayer attacker, LivingEntity target) {
        return CombatResolution.hit(attacker.getId(), target.getId(), 1.0D, 1.0D, false);
    }

    private record DefenderStats(double flee, double critShield, double perfectDodge,
                                 int vit, int agi, int luk, int lvl, double armorEff,
                                 com.etema.ragnarmmo.combat.element.ElementType element) {
    }

    private DefenderStats fetchDefenderStats(LivingEntity target) {
        if (target instanceof ServerPlayer tp) {
            var statsOpt = RagnarCoreAPI.get(tp);
            if (statsOpt.isPresent()) {
                var s = statsOpt.get();
                DerivedStats d = StatResolutionService.computeAuthoritative(tp, s);

                int vit = (int) StatAttributes.getTotal(tp, StatKeys.VIT);
                int agi = (int) StatAttributes.getTotal(tp, StatKeys.AGI);
                int luk = (int) StatAttributes.getTotal(tp, StatKeys.LUK);
                int lvl = s.getLevel();

                double flee = d.flee;
                double pd = d.perfectDodge;
                double criticalShield = Math.floor(lvl / 15.0) + Math.floor(luk / 5.0);
                double armorEff = d.hardDefense;

                return new DefenderStats(flee, criticalShield, pd, vit, agi, luk, lvl, armorEff,
                        com.etema.ragnarmmo.combat.element.ElementType.NEUTRAL);
            }
        } else if (target instanceof net.minecraft.world.entity.Mob mob) {
            CombatMath.TargetStats targetStats = CombatMath.getTargetStats(mob);
            int lvl = CombatMath.tryGetTargetLevel(mob).orElse(0);

            if (lvl > 0) {
                double flee = CombatMath.tryGetResolvedMobFlee(mob)
                        .orElseGet(() -> (int) CombatMath.computeFLEE(targetStats.agi, targetStats.luk, lvl, 0));
                double pd = CombatMath.computePerfectDodge(targetStats.luk);
                double criticalShield = Math.floor(lvl / 15.0) + Math.floor(targetStats.luk / 5.0);
                double armorEff = com.etema.ragnarmmo.player.stats.event.CommonEvents.getArmorEff(mob);
                com.etema.ragnarmmo.combat.element.ElementType element =
                        com.etema.ragnarmmo.combat.element.CombatPropertyResolver.getDefensiveElement(mob);

                return new DefenderStats(
                        flee,
                        criticalShield,
                        pd,
                        targetStats.vit,
                        targetStats.agi,
                        targetStats.luk,
                        lvl,
                        armorEff,
                        element);
            }
        }

        // Neutral baseline for mobs that still do not expose resolved combat stats.
        int lvl = target.getAttributes().hasAttribute(Attributes.MAX_HEALTH)
                ? (int) (target.getMaxHealth() / 10) : 10;
        return new DefenderStats(lvl + CombatMath.FLEE_BASE, lvl / 5.0, 0, lvl, lvl, lvl, lvl, 0,
                com.etema.ragnarmmo.combat.element.ElementType.NEUTRAL);
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

        List<CombatResolution> resolutions = skillResolver.resolveSkill(ctx, actorState, nowTick);

        for (CombatResolution res : resolutions) {
            applyResolution(ctx.actor(), res);
        }

        // Mark cooldown based on skill data (to be expanded)
        cooldownService.markSkillUsed(actorState.getCooldowns(), ctx.skillId(), nowTick, 20);

        return resolutions;
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
                var dmgCalc = RagnarCombatEngine.get().damageCalculator;
                var hitCalc = RagnarCombatEngine.get().hitCalculator;

                double rawDamage = e.getAmount();
                boolean isMagic = com.etema.ragnarmmo.player.stats.event.CommonEvents.isMagicDamage(e.getSource());
                LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity living ? living : null;

                DefenderStats def = RagnarCombatEngine.get().fetchDefenderStats(p);

                // 1. Perfect Dodge
                if (!isMagic && hitCalc.rollPerfectDodge(def.perfectDodge(), p.getRandom())) {
                    e.setAmount(0);
                    e.setCanceled(true);
                    RagnarCombatEngine.get().feedbackService.sendBasicAttackFeedback(
                         null, p, CombatResolution.miss(attacker != null ? attacker.getId() : -1, p.getId())
                    );
                    return;
                }

                // 2. Flee vs Hit (Evasion)
                if (!isMagic && attacker != null) {
                    double accuracy = 0;
                    if (attacker instanceof net.minecraft.world.entity.Mob mob) {
                        var targetStats = CombatMath.getTargetStats(mob);
                        int mobLvl = CombatMath.tryGetTargetLevel(mob).orElse(
                                mob.getAttributes().hasAttribute(Attributes.MAX_HEALTH)
                                        ? (int) (mob.getMaxHealth() / 10) : 10);
                        // Use current 4-parameter signature: (DEX, LUK, level, bonus)
                        accuracy = CombatMath.tryGetResolvedMobHit(mob)
                                .orElseGet(() -> (int) CombatMath.computeHIT(targetStats.dex, targetStats.luk, mobLvl, 0.0));
                    } else if (attacker instanceof ServerPlayer ap) {
                        accuracy = StatResolutionService.computeAuthoritative(ap,
                                RagnarCoreAPI.get(ap).orElse(null)).accuracy;
                    }

                    CombatHitResultType hitResult = hitCalc.rollHitWithCrit(accuracy, def.flee(), 0,
                            def.critShield(), p.getRandom());
                    if (hitResult == CombatHitResultType.MISS) {
                        e.setAmount(0);
                        e.setCanceled(true);
                        RagnarCombatEngine.get().feedbackService.sendBasicAttackFeedback(
                             null, p, CombatResolution.miss(attacker.getId(), p.getId())
                        );
                        return;
                    }
                }

                // 3. Damage Calculation
                double finalDmg = rawDamage;
                if (isMagic) {
                    int intel = (int) StatAttributes.getTotal(p, StatKeys.INT);
                    int dex = (int) StatAttributes.getTotal(p, StatKeys.DEX);
                    double armorMdef = com.etema.ragnarmmo.player.stats.event.CommonEvents.getArmorMagicDefense(p);
                    finalDmg = dmgCalc.applyMagicDefense(rawDamage, intel, def.vit(), dex, def.lvl(), armorMdef);
                } else {
                    finalDmg = dmgCalc.applyPhysicalDefense(rawDamage, def.vit(), def.agi(), def.lvl(), def.armorEff());
                }

                // Elemental
                com.etema.ragnarmmo.combat.element.ElementType attackElement =
                        com.etema.ragnarmmo.player.stats.event.CommonEvents.resolveIncomingAttackElement(
                                attacker, e.getSource().getDirectEntity(), isMagic);
                finalDmg *= com.etema.ragnarmmo.combat.element.CombatPropertyResolver.getElementalModifier(
                        attackElement, def.element());

                e.setAmount((float) Math.max(1.0, finalDmg));
                DamageProcessingGuard.markProcessedPlayer(p);
            });
        }
    }
}
