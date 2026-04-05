package com.etema.ragnarmmo.skill.job.wizard;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaterBallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:water_ball");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Water Ball: Shoots multiple water spheres if near water.
        // For Minecraft, we'll check if player is in water or it's raining.

        boolean nearWater = player.isInWater() || player.level().isRainingAt(player.blockPosition());
        if (!nearWater)
            return;

        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            int balls = 1 + (level * 2);
            for (int i = 0; i < balls; i++) {
                int delay = i * 2;
                com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(delay, () -> {
                    Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
                    Vec3 shootDir;
                    
                    if (target != null && target.isAlive()) {
                        Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2.0, 0);
                        shootDir = targetVec.subtract(startPos).normalize();
                    } else {
                        shootDir = player.getLookAngle();
                    }

                    com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                        new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, 2.0f + (level * 0.5f), ParticleTypes.SPLASH);
                    
                    projectile.setProjectileType("waterball");
                    projectile.setHoming(false);
                    projectile.setGravity(0.02f); // Slight gravity for water balls
                    
                    projectile.setPos(startPos.x, startPos.y, startPos.z);
                    projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.5f, 2.0f); // Fast and slightly spread
                    
                    projectile.setOnHitEffect(result -> {
                        serverLevel.sendParticles(ParticleTypes.SPLASH, result.getLocation().x, result.getLocation().y, result.getLocation().z, 10, 0.3, 0.3, 0.3, 0.1);
                        serverLevel.playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8f, 1.2f);
                    });

                    player.level().addFreshEntity(projectile);
                });
            }
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
