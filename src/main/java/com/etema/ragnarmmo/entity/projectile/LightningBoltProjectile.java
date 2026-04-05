package com.etema.ragnarmmo.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;
import com.etema.ragnarmmo.common.init.RagnarEntities;
import net.minecraft.sounds.SoundEvent;

public class LightningBoltProjectile extends AbstractMagicProjectile {

    public LightningBoltProjectile(EntityType<? extends LightningBoltProjectile> type, Level level) {
        super(type, level);
    }

    public LightningBoltProjectile(Level level, LivingEntity owner, float damage) {
        super(RagnarEntities.LIGHTNING_BOLT_PROJECTILE.get(), level, owner, damage);
    }

    @Override
    public void trailParticles() {
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.GLOW, getX() + (random.nextDouble() - 0.5) * 0.3, getY(), getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 20, 0.2, 0.8, 0.2, 0.1);
            level().playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    @Override
    public float getSpeed() {
        return 1.5f;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_IMPACT);
    }
}
