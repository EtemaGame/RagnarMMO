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
import com.etema.ragnarmmo.combat.api.CombatTargetCandidate;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import com.etema.ragnarmmo.system.stats.compute.StatComputer;
import com.etema.ragnarmmo.system.stats.compute.StatResolutionService;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.util.DamageProcessingGuard;

import net.minecraft.server.level.ServerPlayer;
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
        ServerPlayer attacker = ctx.actor();
        long nowTick = attacker.serverLevel().getGameTime();
        CombatActorState actorState = state(attacker);

        String reject = validationService.validateBasicAttack(ctx, actorState, nowTick, cooldownService);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, reject);
            return Collections.emptyList();
        }

        actorState.setLastAcceptedSequenceId(ctx.sequenceId());

        // Attacker Data (Authoritative)
        var attackerStats = RagnarCoreAPI.get(attacker).orElse(null);
        if (attackerStats == null) return Collections.emptyList();

        DerivedStats attackerDerived = StatResolutionService.computeAuthoritative(attacker, attackerStats);
        if (attackerDerived == null) return Collections.emptyList();

        // ASPD math determines cooldown ticks
        double aps = CombatMath.convertASPD_ToAPS((int) attackerDerived.attackSpeed);
        int cooldownTicks = (int) Math.max(2, 20.0 / aps);
        cooldownService.markBasicAttackUsed(actorState.getCooldowns(), nowTick, cooldownTicks);

        List<CombatResolution> results = new ArrayList<>();
        for (CombatTargetCandidate candidate : ctx.candidates()) {
            CombatResolution resolution = resolveSingleBasicHit(attacker, attackerDerived, candidate);
            results.add(resolution);
            applyResolution(attacker, resolution);
            CombatDebugLog.logHitResolution(resolution);
        }
        return results;
    }

    private CombatResolution resolveSingleBasicHit(ServerPlayer attacker, DerivedStats attackerDerived, CombatTargetCandidate candidate) {
        net.minecraft.world.entity.Entity targetEntity = attacker.serverLevel().getEntity(candidate.entityId());
        if (!(targetEntity instanceof LivingEntity target)) {
            return CombatResolution.miss(attacker.getId(), candidate.entityId());
        }

        // Attacker Data from authoritative source
        int lvl = (int) StatAttributes.getTotal(attacker, StatKeys.LEVEL);
        ItemStack weapon = attacker.getMainHandItem();
        
        double totalBaseAtk = attackerDerived.physicalAttack;
        double accuracy = attackerDerived.accuracy;
        double critChance = attackerDerived.criticalChance;

        // Defender Data
        DefenderStats def = fetchDefenderStats(target);

        // 1. Perfect Dodge check
        if (hitCalculator.rollPerfectDodge(def.perfectDodge(), attacker.getRandom())) {
            return CombatResolution.miss(attacker.getId(), target.getId());
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
        int str = (int) StatAttributes.getTotal(attacker, StatKeys.STR);

        double dmg = damageCalculator.computePhysicalDamage(totalBaseAtk, dex, luk,
                new java.util.Random(attacker.getRandom().nextLong()));

        if (hitResult == CombatHitResultType.CRIT) {
            dmg = damageCalculator.applyCriticalModifier(dmg, luk, str);
        }

        // SIZE & ELEMENTAL
        var attackElement = com.etema.ragnarmmo.combat.element.CombatPropertyResolver.getOffensiveElement(attacker);
        dmg = damageCalculator.applyModifiers(dmg, weapon, target, attackElement, false);

        // DEFENSE
        dmg = damageCalculator.applyPhysicalDefense(dmg, def.vit(), def.agi(), lvl, def.armorEff());

        return CombatResolution.hit(
                attacker.getId(),
                target.getId(),
                dmg,
                dmg,
                hitResult == CombatHitResultType.CRIT);
    }

    private record DefenderStats(double flee, double critShield, double perfectDodge, 
                                 int vit, int agi, int luk, int lvl, double armorEff) {}

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
                double armorEff = d.defense; 
                
                return new DefenderStats(flee, criticalShield, pd, vit, agi, luk, lvl, armorEff);
            }
        } else if (target instanceof net.minecraft.world.entity.Mob mob) {
            var mobStatsOpt = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(mob);
            var ms = mobStatsOpt.orElse(null);
            if (ms != null) {
                int vit = (int) ms.get(StatKeys.VIT);
                int agi = (int) ms.get(StatKeys.AGI);
                int luk = (int) ms.get(StatKeys.LUK);
                int lvl = ms.getLevel();
                
                double flee = CombatMath.computeFLEE(agi, luk, lvl, 0);
                double pd = CombatMath.computePerfectDodge(luk);
                double criticalShield = Math.floor(lvl / 15.0) + Math.floor(luk / 5.0);
                double armorEff = com.etema.ragnarmmo.system.stats.event.CommonEvents.getArmorEff(mob);
                com.etema.ragnarmmo.combat.element.ElementType element = ms.getElement();
                
                return new DefenderStats(flee, criticalShield, pd, vit, agi, luk, lvl, armorEff, element);
            }
        }
        
        // Fallback for vanilla mobs without stats
        int lvl = target.getAttributes().hasAttribute(Attributes.MAX_HEALTH) ? (int) (target.getMaxHealth() / 10) : 10;
        return new DefenderStats(lvl + CombatMath.FLEE_BASE, lvl / 5.0, 0, lvl, lvl, lvl, lvl, 0, com.etema.ragnarmmo.combat.element.ElementType.NEUTRAL);
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
        
        List<CombatResolution> resolutions = skillResolver.resolveSkill(ctx, actorState, nowTick);
        
        for (CombatResolution res : resolutions) {
            applyResolution(ctx.actor(), res);
        }

        // Mark cooldown based on skill data (to be expanded)
        cooldownService.markSkillUsed(actorState.getCooldowns(), ctx.skillId(), nowTick, 20);
        
        return resolutions;
    }

    private void applyResolution(ServerPlayer attacker, CombatResolution resolution) {
        net.minecraft.world.entity.Entity target = attacker.serverLevel().getEntity(resolution.targetId());
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        if (resolution.resultType() == CombatHitResultType.HIT || resolution.resultType() == CombatHitResultType.CRIT) {
            float damage = (float) resolution.finalAmount();
            
            // Mark as processed to avoid CommonEvents double-calc
            DamageProcessingGuard.markProcessedPlayer(livingTarget);
            
            // Use MobAttack source to represent authoritative damage
            livingTarget.hurt(attacker.damageSources().mobAttack(attacker), damage);
        }

        feedbackService.sendBasicAttackFeedback(attacker, livingTarget, resolution);
    }
}
