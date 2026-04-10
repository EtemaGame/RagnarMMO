package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

final class SwordmanCombatUtil {

    private static final String SKILL_DAMAGE_CONTEXT_TAG = "ragnar_skill_damage_context";

    private SwordmanCombatUtil() {
    }

    static int estimateLevel(LivingEntity entity) {
        return CombatMath.tryGetTargetLevel(entity)
                .orElse(Math.max(1, (int) (entity.getMaxHealth() / 10.0)));
    }

    static boolean rollPhysicalSkillHit(LivingEntity attacker, LivingEntity target, double flatHitBonus, double hitMultiplier) {
        double attackerHit = estimateHit(attacker, flatHitBonus) * hitMultiplier;
        double defenderFlee = estimateFlee(target);
        double hitRate = CombatMath.computeHitRate(attackerHit, defenderFlee);
        return CombatMath.rollHit(hitRate, attacker.getRandom());
    }

    static void withSkillDamageContext(LivingEntity attacker, Runnable action) {
        int previousDepth = attacker.getPersistentData().getInt(SKILL_DAMAGE_CONTEXT_TAG);
        attacker.getPersistentData().putInt(SKILL_DAMAGE_CONTEXT_TAG, previousDepth + 1);
        try {
            action.run();
        } finally {
            if (previousDepth <= 0) {
                attacker.getPersistentData().remove(SKILL_DAMAGE_CONTEXT_TAG);
            } else {
                attacker.getPersistentData().putInt(SKILL_DAMAGE_CONTEXT_TAG, previousDepth);
            }
        }
    }

    static boolean isSkillDamageContext(LivingEntity attacker) {
        return attacker.getPersistentData().getInt(SKILL_DAMAGE_CONTEXT_TAG) > 0;
    }

    private static double estimateHit(LivingEntity entity, double bonus) {
        if (entity instanceof Player player) {
            return RagnarCoreAPI.get(player)
                    .map(stats -> CombatMath.computeHIT(stats.getDEX(), stats.getLUK(), stats.getLevel(), bonus))
                    .orElse(CombatMath.HIT_BASE + estimateLevel(entity) + bonus);
        }

        var resolvedHit = CombatMath.tryGetResolvedMobHit(entity);
        if (resolvedHit.isPresent()) {
            return resolvedHit.getAsInt() + bonus;
        }

        CombatMath.TargetStats stats = CombatMath.getTargetStats(entity);
        int level = estimateLevel(entity);
        if (level > 0) {
            return CombatMath.computeHIT(stats.dex, stats.luk, level, bonus);
        }

        return CombatMath.HIT_BASE + estimateLevel(entity) + bonus;
    }

    private static double estimateFlee(LivingEntity entity) {
        if (entity instanceof Player player) {
            return RagnarCoreAPI.get(player)
                    .map(stats -> CombatMath.computeFLEE(stats.getAGI(), stats.getLUK(), stats.getLevel(), 0))
                    .orElse(CombatMath.FLEE_BASE + estimateLevel(entity));
        }

        var resolvedFlee = CombatMath.tryGetResolvedMobFlee(entity);
        if (resolvedFlee.isPresent()) {
            return resolvedFlee.getAsInt();
        }

        CombatMath.TargetStats stats = CombatMath.getTargetStats(entity);
        int level = estimateLevel(entity);
        if (level > 0) {
            return CombatMath.computeFLEE(stats.agi, stats.luk, level, 0);
        }

        return CombatMath.FLEE_BASE + estimateLevel(entity);
    }
}
