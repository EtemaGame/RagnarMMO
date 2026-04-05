package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.job.mage.MageTargetUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Napalm Beat — Ghost property splash attack.
 * Causes damage to enemies within a 9 cell area (3x3 / 1.5 radius) around the target.
 * Non-linear SP cost and Cast Delay scaling.
 */
public class NapalmBeatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "napalm_beat");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getResourceCost(int level, int defaultCost) {
        // Lv 1-3: 9
        // Lv 4-6: 12
        // Lv 7-9: 15
        // Lv 10: 18
        if (level >= 10) return 18;
        if (level >= 7) return 15;
        if (level >= 4) return 12;
        return 9;
    }

    @Override
    public int getCastDelay(int level) {
        // Lv 1-3: 1.0s (20t)
        // Lv 4-5: 0.9s (18t)
        // Lv 6-7: 0.8s (16t)
        // Lv 8: 0.7s (14t)
        // Lv 9: 0.6s (12t)
        // Lv 10: 0.5s (10t)
        return switch (level) {
            case 1, 2, 3 -> 20;
            case 4, 5 -> 18;
            case 6, 7 -> 16;
            case 8 -> 14;
            case 9 -> 12;
            case 10 -> 10;
            default -> 20;
        };
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity mainTarget = MageTargetUtil.raycast(player, 10.0);
        if (mainTarget == null) return;

        // Damage: 0.8 + (level-1)*0.1 MATK
        float matkPercent = 80.0f + (level - 1) * 10.0f;
        float damage = SkillDamageHelper.scaleByMATK(player, matkPercent);

        // Visuals on primary target
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WITCH, mainTarget.getX(), mainTarget.getY() + 1.0, mainTarget.getZ(), 20, 0.2, 0.4, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.SOUL, mainTarget.getX(), mainTarget.getY() + 1.0, mainTarget.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, mainTarget.getX(), mainTarget.getY(), mainTarget.getZ(), 
                    SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.5f);
        }

        // Area Damage (~1.5 block radius = approx 3x3 cells)
        AABB area = mainTarget.getBoundingBox().inflate(1.5);
        List<Entity> targets = player.level().getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive());

        for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
                SkillDamageHelper.dealSkillDamage(living, player.level().damageSources().magic(), damage);
                
                // Small splash visual for extra targets
                if (player.level() instanceof ServerLevel sl && living != mainTarget) {
                    sl.sendParticles(ParticleTypes.WITCH, living.getX(), living.getY() + 1.0, living.getZ(), 8, 0.1, 0.2, 0.1, 0.01);
                }
            }
        }
    }
}
