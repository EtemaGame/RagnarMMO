package com.etema.ragnarmmo.combat.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.contract.ActionIntent;
import com.etema.ragnarmmo.combat.contract.CombatContract;
import com.etema.ragnarmmo.combat.contract.CombatStrictMode;
import com.etema.ragnarmmo.combat.contract.CombatantProfile;
import com.etema.ragnarmmo.combat.contract.CombatantProfileResolver;
import com.etema.ragnarmmo.combat.contract.SkillCombatSpec;
import com.etema.ragnarmmo.combat.contract.SkillCombatSpecResolver;
import com.etema.ragnarmmo.combat.profile.HandAttackProfileResolver;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;
import com.etema.ragnarmmo.common.api.player.RoPlayerDataAccess;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Resolves active skill usage within the authoritative combat pipeline.
 */
public class RagnarSkillResolver {

    private final RagnarHitCalculator hitCalculator;
    private final RagnarDamageCalculator damageCalculator;
    private final CombatContract combatContract;

    public RagnarSkillResolver(RagnarHitCalculator hitCalculator, RagnarDamageCalculator damageCalculator,
            CombatContract combatContract) {
        this.hitCalculator = hitCalculator;
        this.damageCalculator = damageCalculator;
        this.combatContract = combatContract;
    }

    public List<CombatResolution> resolveSkill(CombatRequestContext ctx, CombatActorState actorState, long nowTick) {
        ServerPlayer player = ctx.actor();
        String skillIdStr = ctx.skillId();
        if (skillIdStr == null || skillIdStr.isBlank()) {
            return Collections.emptyList();
        }

        ResourceLocation skillId = skillIdStr.contains(":")
                ? ResourceLocation.parse(skillIdStr)
                : ResourceLocation.fromNamespaceAndPath(com.etema.ragnarmmo.RagnarMMO.MODID, skillIdStr);
        
        Optional<SkillDefinition> defOpt = SkillRegistry.get(skillId);
        if (defOpt.isEmpty()) {
            return Collections.emptyList();
        }

        SkillDefinition def = defOpt.get();
        Optional<ISkillEffect> effectOpt = SkillRegistry.getEffect(skillId);

        // 1. Get level from metadata
        int level = 1;
        if (ctx.metadata().get("level") instanceof Integer l) {
            level = l;
        }

        final int finalLevel = level;

        Optional<SkillCombatSpec> specOpt = SkillCombatSpecResolver.resolve(def, finalLevel);
        if (specOpt.isPresent()) {
            SkillCombatSpec spec = specOpt.get();
            List<CombatResolution> results = new ArrayList<>();

            Map<Integer, LivingEntity> targets = resolveSkillTargets(ctx, spec);
            Integer primaryTargetId = ctx.candidates().isEmpty() ? null : ctx.candidates().get(0).entityId();
            for (LivingEntity target : targets.values()) {
                if (isSkillTargetBlocked(skillId, target)) {
                    CombatDebugLog.logValidationReject(ctx, "SKILL_TARGET_BLOCKED");
                    continue;
                }

                CombatantProfile attackerProfile = CombatantProfileResolver
                        .resolvePlayer(player, HandAttackProfileResolver.resolve(player, ctx.offHand()).orElse(null))
                        .orElse(null);
                CombatantProfile defenderProfile = target instanceof ServerPlayer defenderPlayer
                        ? CombatantProfileResolver.resolvePlayer(defenderPlayer, null).orElse(null)
                        : target instanceof net.minecraft.world.entity.Mob mob
                                ? CombatantProfileResolver.resolveMob(mob, CombatStrictMode.current()).orElse(null)
                                : null;

                SkillCombatSpec effectiveSpec = spec;
                if (primaryTargetId != null && target.getId() != primaryTargetId && spec.aoeRadius() > 0.0D) {
                    effectiveSpec = new SkillCombatSpec(
                            spec.damageType(),
                            spec.element(),
                            spec.hitPolicy(),
                            spec.damagePercent() * spec.splashRatio(),
                            spec.hitCount(),
                            0.0D,
                            1.0D);
                }

                var contractResult = combatContract.resolveSkill(
                        attackerProfile,
                        defenderProfile,
                        new ActionIntent.SkillIntent(skillId, finalLevel, target.getId()),
                        effectiveSpec,
                        deterministicRandom(player, target, ctx.sequenceId() + finalLevel));
                if (contractResult.rejected() || contractResult.resolution() == null) {
                    CombatDebugLog.logValidationReject(ctx, "SKILL_CONTRACT_REJECTED_" + contractResult.rejectReason());
                    continue;
                }
                results.add(contractResult.resolution());
            }

            if (SkillCombatSpecResolver.shouldExecuteLegacyEffectAfterContract(skillId)) {
                effectOpt.ifPresent(effect -> {
                    try {
                        effect.execute(player, finalLevel);
                    } catch (Exception e) {
                        CombatDebugLog.logValidationReject(ctx, "Effect Execution Error: " + e.getMessage());
                    }
                });
            }

            return results;
        }

        effectOpt.ifPresent(effect -> {
            try {
                effect.execute(player, finalLevel);
            } catch (Exception e) {
                CombatDebugLog.logValidationReject(ctx, "Effect Execution Error: " + e.getMessage());
            }
        });
        return Collections.emptyList();
    }

    /**
     * Policy: Aggregated Multihit
     * Resolves N subhits into a single resolution or controlled packet stream.
     */
    public List<CombatResolution> resolveAggregatedMultiHit(CombatRequestContext ctx, int hitCount, double damageMultiplier) {
        ServerPlayer player = ctx.actor();
        return RoPlayerDataAccess.get(player).map(data -> {
            IPlayerStats stats = data.getStats();
            List<CombatResolution> results = new ArrayList<>();

            for (var candidate : ctx.candidates()) {
                net.minecraft.world.entity.Entity targetEntity = player.serverLevel().getEntity(candidate.entityId());
                if (!(targetEntity instanceof net.minecraft.world.entity.LivingEntity target)) continue;

                // For multihit, we roll for each sub-hit
                double attackerHit = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeHIT(stats.getDEX(), stats.getLUK(), stats.getLevel(), 0);
                int landedHits = hitCalculator.rollMultiHit(hitCount, attackerHit, 50.0, player.getRandom()); 
                
                if (landedHits <= 0) {
                    results.add(CombatResolution.miss(player.getId(), target.getId()));
                } else {
                    double weaponBaseAtk = com.etema.ragnarmmo.player.stats.event.CommonEvents.getWeaponDamage(player);
                    boolean isRanged = com.etema.ragnarmmo.player.stats.compute.CombatMath.isRangedWeapon(player.getMainHandItem());
                    double baseAtk = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeTotalATK(stats.getSTR(), stats.getDEX(), stats.getLUK(), stats.getLevel(), weaponBaseAtk, 0, isRanged);
                    double totalDamage = 0;
                    for (int i = 0; i < landedHits; i++) {
                        totalDamage += damageCalculator.computePhysicalDamage(baseAtk, stats.getDEX(), stats.getLUK(), new java.util.Random(player.getRandom().nextLong()));
                    }
                    totalDamage *= damageMultiplier;
                    results.add(CombatResolution.hit(player.getId(), target.getId(), landedHits, totalDamage, false));
                }
            }
            return results;
        }).orElse(Collections.emptyList());
    }

    private static java.util.Random deterministicRandom(ServerPlayer attacker, LivingEntity target, int sequenceSalt) {
        long seed = 37L * attacker.serverLevel().getGameTime()
                + 19L * attacker.getId()
                + 11L * target.getId()
                + sequenceSalt;
        return new java.util.Random(seed);
    }

    private static boolean isSkillTargetBlocked(ResourceLocation skillId, LivingEntity target) {
        if (skillId == null) {
            return false;
        }
        return switch (skillId.getPath()) {
            case "frost_diver" -> target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD
                    || com.etema.ragnarmmo.mobs.util.MobUtils.isBossLike(target);
            default -> false;
        };
    }

    private static Map<Integer, LivingEntity> resolveSkillTargets(CombatRequestContext ctx, SkillCombatSpec spec) {
        ServerPlayer player = ctx.actor();
        Map<Integer, LivingEntity> targets = new LinkedHashMap<>();
        LivingEntity primary = null;
        for (var candidate : ctx.candidates()) {
            net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(candidate.entityId());
            if (entity instanceof LivingEntity living && living.isAlive() && living != player) {
                targets.put(living.getId(), living);
                if (primary == null) {
                    primary = living;
                }
            }
        }

        if (spec.aoeRadius() > 0.0D) {
            var area = primary != null
                    ? primary.getBoundingBox().inflate(spec.aoeRadius())
                    : player.getBoundingBox().inflate(spec.aoeRadius());
            for (LivingEntity living : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity.isAlive() && entity != player)) {
                targets.putIfAbsent(living.getId(), living);
            }
        }
        return targets;
    }
}

