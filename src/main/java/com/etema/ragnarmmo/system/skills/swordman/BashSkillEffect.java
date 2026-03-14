package com.etema.ragnarmmo.system.skills.swordman;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.skills.SkillProgress;
import com.etema.ragnarmmo.system.skills.SkillProgressManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BashSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "bash");
    private static final ResourceLocation FATAL_BLOW = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fatal_blow");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getMeleeTarget(player);
        if (target == null)
            return;

        // Base RO formula: (100 + 30 * level)% ATK
        float damageMultiplier = 1.0f + (level * 0.3f);
        final float baseDamage = 4.0f * damageMultiplier;

        // Introduce a slight delay for the "swing" feel
        com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(3, () -> {
            if (!target.isAlive()) return;

            target.hurt(player.damageSources().mobAttack(player), baseDamage);

            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Sweep effect for the swing
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, 
                        player.getX() + player.getLookAngle().x, 
                        player.getY() + 1.2, 
                        player.getZ() + player.getLookAngle().z, 
                        1, 0, 0, 0, 0);

                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(),
                        20, 0.3, 0.5, 0.3, 0.2);
            }

            // Fatal Blow check
            if (level >= 6) {
                SkillProgress fbProgress = SkillProgressManager.getProgress(player, FATAL_BLOW);
                if (fbProgress != null && fbProgress.getLevel() > 0) {
                    float stunChance = 0.05f * (level - 5);
                    if (player.getRandom().nextFloat() <= stunChance) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, true, true));
                        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, true, false));
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                            sLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getY() + 2.0,
                                    target.getZ(), 5, 0.3, 0.2, 0.3, 0);
                        }
                    }
                }
            }
        });
    }

    private LivingEntity getMeleeTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(3.5));

        AABB searchBox = player.getBoundingBox().inflate(3.5);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
