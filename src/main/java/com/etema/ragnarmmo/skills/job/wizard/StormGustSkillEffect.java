package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class StormGustSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "storm_gust");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 1.0f,
                    0.5f);

            int projectileCount = 10 + level;
            for (int i = 0; i < projectileCount; i++) {
                int delay = i * 2;
                com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(delay, () -> {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double dist = 2.0 + RANDOM.nextDouble() * 5.0;
                    double x = player.getX() + Math.cos(angle) * dist;
                    double z = player.getZ() + Math.sin(angle) * dist;
                    double y = player.getY() + 10.0;

                    // RO: Each hit deals 100% MATK
                    float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 100.0f);
                    com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                        new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, damage, ParticleTypes.SNOWFLAKE);
                    
                    projectile.setProjectileType("storm_gust");
                    projectile.setHoming(false);
                    projectile.setGravity(0.03f);
                    
                    projectile.setPos(x, y, z);
                    projectile.shoot(0, -1, 0, 0.8f, 0.0f); // Falling snowflakes
                    
                    projectile.setOnHitEffect(result -> {
                        if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                            target.knockback(1.0, player.getX() - target.getX(), player.getZ() - target.getZ());
                        }
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, result.getLocation().x, result.getLocation().y, result.getLocation().z, 20, 0.5, 0.5, 0.5, 0.05);
                    });

                    player.level().addFreshEntity(projectile);
                });
            }
        }
    }
}
