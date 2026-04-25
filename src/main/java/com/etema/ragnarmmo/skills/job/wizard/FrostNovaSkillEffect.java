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

import java.util.List;

/**
 * Frost Nova — Active (Water/Ice AoE)
 * RO: Freezes enemies in a small area around the caster. 30% freeze chance.
 *
 * Fixed from original: missing freeze proc implementation.
 */
public class FrostNovaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "frost_nova");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        AABB area = player.getBoundingBox().inflate(3.0 + (level * 0.5));
        if (!(player.level() instanceof ServerLevel sl)) return;

        sl.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        sl.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);

        int projectileCount = 8 + (level * 2);
        for (int i = 0; i < projectileCount; i++) {
            double angle = (i * Math.PI * 2) / projectileCount;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);

            com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, 4.0f + (level * 2.0f), ParticleTypes.SNOWFLAKE);
            
            projectile.setProjectileType("frost_nova");
            projectile.setHoming(false);
            projectile.setGravity(0.0f);
            
            projectile.setPos(player.getX(), player.getY() + 1.0, player.getZ());
            projectile.shoot(vx, 0, vz, 0.6f, 0.0f); // Slow outward burst
            
            projectile.setOnHitEffect(result -> {
                if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));
                    float freezeChance = 0.30f + (0.05f * level);
                    if (player.getRandom().nextFloat() < freezeChance) {
                        int freezeTicks = (3 + level) * 20;
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, 10, false, true, true));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, freezeTicks, 1, false, true, false));
                        target.setTicksFrozen(freezeTicks);
                    }
                }
                sl.sendParticles(ParticleTypes.SNOWFLAKE, result.getLocation().x, result.getLocation().y, result.getLocation().z, 10, 0.2, 0.2, 0.2, 0.02);
            });

            player.level().addFreshEntity(projectile);
        }
    }
}
