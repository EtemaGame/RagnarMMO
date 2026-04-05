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

public class FireBoltProjectile extends AbstractMagicProjectile {

    public FireBoltProjectile(EntityType<? extends FireBoltProjectile> type, Level level) {
        super(type, level);
    }

    public FireBoltProjectile(Level level, LivingEntity owner, float damage) {
        super(RagnarEntities.FIRE_BOLT_PROJECTILE.get(), level, owner, damage);
    }

    @Override
    public void trailParticles() {
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0.05, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.2, getY(), getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.3, 0.3, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 5, 0.2, 0.2, 0.2, 0.05);
            level().playSound(null, x, y, z, com.etema.ragnarmmo.common.init.RagnarSounds.BOLT_HIT.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    @Override
    public float getSpeed() {
        return 1.2f;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.of(com.etema.ragnarmmo.common.init.RagnarSounds.BOLT_HIT.get());
    }
}
