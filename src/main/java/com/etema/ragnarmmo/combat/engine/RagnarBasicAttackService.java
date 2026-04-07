package com.etema.ragnarmmo.combat.engine;

import java.util.Collection;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * First practical server-authoritative basic attack slice.
 * <p>
 * This intentionally avoids vanilla melee authority and uses RagnarMMO stats to
 * decide hit/crit/damage. It is conservative and meant as the first migration
 * step, not the final complete RO combat model.
 */
public class RagnarBasicAttackService {
    private final RagnarCombatCooldownService cooldownService = new RagnarCombatCooldownService();
    private final RagnarCombatValidationService validationService = new RagnarCombatValidationService();

    public CombatResolution tryResolve(ServerPlayer attacker, LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return null;
        }

        CombatActorState actorState = RagnarCombatEngine.get().state(attacker);
        long nowTick = attacker.serverLevel().getGameTime();
        var ctx = new com.etema.ragnarmmo.combat.api.CombatRequestContext(
                attacker,
                com.etema.ragnarmmo.combat.api.CombatActionType.BASIC_ATTACK,
                actorState.getLastAcceptedSequenceId() + 1,
                0,
                false,
                attacker.getInventory().selected,
                null,
                java.util.List.of(com.etema.ragnarmmo.combat.api.CombatTargetCandidate.betterCombat(target.getId(),
                        attacker.distanceTo(target))));

        String reject = validationService.validateBasicAttack(ctx, actorState, nowTick, cooldownService);
        if (reject != null) {
            CombatDebugLog.logValidationReject(ctx, reject);
            return null;
        }

        var statsOpt = RagnarCoreAPI.get(attacker);
        if (statsOpt.isEmpty()) {
            CombatDebugLog.logValidationReject(ctx, "missing_attacker_stats");
            return null;
        }

        IPlayerStats attackerStats = statsOpt.get();
        double weaponAtk = extractMainhandAttackDamage(attacker);
        DerivedStats attackerDerived = RagnarCoreAPI
                .computeDerivedStats(attacker, attackerStats, weaponAtk, 1.0D, 0.0D, target.getArmorValue(), 1.0D)
                .orElse(null);
        if (attackerDerived == null) {
            CombatDebugLog.logValidationReject(ctx, "missing_attacker_derived_stats");
            return null;
        }

        double targetFlee = 0.0D;
        double targetPhysicalReduction = estimateTargetPhysicalReduction(target);
        if (target instanceof ServerPlayer targetPlayer) {
            var targetStatsOpt = RagnarCoreAPI.get(targetPlayer);
            if (targetStatsOpt.isPresent()) {
                DerivedStats targetDerived = RagnarCoreAPI
                        .computeDerivedStats(targetPlayer, targetStatsOpt.get(), 0.0D, 1.0D, 0.0D,
                                targetPlayer.getArmorValue(), 1.0D)
                        .orElse(null);
                if (targetDerived != null) {
                    targetFlee = targetDerived.flee;
                    targetPhysicalReduction = Math.max(targetPhysicalReduction, targetDerived.physicalDamageReduction);
                }
            }
        }

        boolean hit = rollAgainstPercent(attackerDerived.accuracy - targetFlee, attacker.getRandom().nextDouble());
        actorState.setLastAcceptedSequenceId(ctx.sequenceId());
        int intervalTicks = Math.max(1, (int) Math.round(Math.max(0.05D, attackerDerived.globalCooldown) * 20.0D));
        cooldownService.markBasicAttackUsed(actorState.getCooldowns(), nowTick, intervalTicks);

        if (!hit) {
            return CombatResolution.miss(attacker.getId(), target.getId());
        }

        boolean critical = rollAgainstPercent(attackerDerived.criticalChance, attacker.getRandom().nextDouble());
        double baseDamage = Math.max(attackerDerived.physicalAttackMin,
                (attackerDerived.physicalAttackMin + attackerDerived.physicalAttackMax) * 0.5D);
        double damage = Math.max(0.0D, baseDamage * (1.0D - clamp01(targetPhysicalReduction)));
        if (critical) {
            damage *= Math.max(1.0D, attackerDerived.criticalDamageMultiplier);
        }

        boolean hurt = target.hurt(attacker.damageSources().playerAttack(attacker), (float) damage);
        if (!hurt) {
            return CombatResolution.miss(attacker.getId(), target.getId());
        }

        return CombatResolution.hit(attacker.getId(), target.getId(), baseDamage, damage, critical);
    }

    private static boolean rollAgainstPercent(double chancePercent, double random01) {
        double clamped = Math.max(0.0D, Math.min(100.0D, chancePercent));
        return random01 * 100.0D < clamped;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(0.95D, value));
    }

    private static double estimateTargetPhysicalReduction(LivingEntity target) {
        double armor = Math.max(0.0D, target.getArmorValue());
        return Math.min(0.80D, armor / 100.0D);
    }

    private static double extractMainhandAttackDamage(ServerPlayer player) {
        if (player == null || player.getMainHandItem().isEmpty()) {
            return 0.0D;
        }
        Collection<AttributeModifier> modifiers = player.getMainHandItem()
                .getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);
        double total = 0.0D;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                total += modifier.getAmount();
            }
        }
        return Math.max(0.0D, total);
    }
}
