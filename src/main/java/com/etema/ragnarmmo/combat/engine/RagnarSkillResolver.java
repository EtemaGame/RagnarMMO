package com.etema.ragnarmmo.combat.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatResolution;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import com.etema.ragnarmmo.combat.util.CombatDebugLog;
import com.etema.ragnarmmo.common.api.player.RoPlayerDataAccess;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves active skill usage within the authoritative combat pipeline.
 */
public class RagnarSkillResolver {

    private final RagnarHitCalculator hitCalculator;
    private final RagnarDamageCalculator damageCalculator;

    public RagnarSkillResolver(RagnarHitCalculator hitCalculator, RagnarDamageCalculator damageCalculator) {
        this.hitCalculator = hitCalculator;
        this.damageCalculator = damageCalculator;
    }

    public List<CombatResolution> resolveSkill(CombatRequestContext ctx, CombatActorState actorState, long nowTick) {
        ServerPlayer player = ctx.actor();
        String skillIdStr = ctx.skillId();
        if (skillIdStr == null || skillIdStr.isBlank()) {
            return Collections.emptyList();
        }

        ResourceLocation skillId = skillIdStr.contains(":") ? new ResourceLocation(skillIdStr) : new ResourceLocation(com.etema.ragnarmmo.RagnarMMO.MODID, skillIdStr);
        
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

        // 2. Resolve Targets and Hits
        return RoPlayerDataAccess.get(player).map(data -> {
            IPlayerStats stats = data.getStats();
            List<CombatResolution> results = new ArrayList<>();
            
            for (var candidate : ctx.candidates()) {
                net.minecraft.world.entity.Entity targetEntity = player.serverLevel().getEntity(candidate.entityId());
                if (!(targetEntity instanceof net.minecraft.world.entity.LivingEntity target)) continue;

                // Base stats for skill calculation
                int dex = stats.getDEX();
                int luk = stats.getLUK();
                int str = stats.getSTR();
                int intel = stats.getINT();
                int lvl = stats.getLevel();

                // Temporary skill multiplier logic until per-skill authored formulas are wired in.
                double damageMultiplier = 1.0 + (finalLevel * 0.2);
                
                // Fetch Defender Stats (Ideally reuse the logic from RagnarCombatEngine)
                // For simplicity in this step, I will assume a base hit until fully refactored
                double attackerHit = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeHIT(stats.getDEX(), stats.getLUK(), stats.getLevel(), 0);
                
                // Temporary simplified resolution - to be expanded with full defender stats
                double weaponBaseAtk = com.etema.ragnarmmo.player.stats.event.CommonEvents.getWeaponDamage(player);
                boolean isRanged = com.etema.ragnarmmo.player.stats.compute.CombatMath.isRangedWeapon(player.getMainHandItem());
                double baseDamage = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeTotalATK(stats.getSTR(), stats.getDEX(), stats.getLUK(), stats.getLevel(), weaponBaseAtk, 0, isRanged);
                double skillDmg = damageCalculator.computePhysicalDamage(baseDamage, dex, luk, new java.util.Random(player.getRandom().nextLong()));
                skillDmg *= damageMultiplier;

                results.add(CombatResolution.hit(player.getId(), target.getId(), 1.0, skillDmg, false));
            }

            // 3. Trigger actual effect logic if available
            effectOpt.ifPresent(effect -> {
                try {
                    effect.execute(player, finalLevel);
                } catch (Exception e) {
                    CombatDebugLog.logValidationReject(ctx, "Effect Execution Error: " + e.getMessage());
                }
            });

            return results;
        }).orElse(Collections.emptyList());
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
}

