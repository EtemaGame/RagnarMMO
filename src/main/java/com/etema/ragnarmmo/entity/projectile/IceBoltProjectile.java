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

public class IceBoltProjectile extends AbstractMagicProjectile {

    public IceBoltProjectile(EntityType<? extends IceBoltProjectile> type, Level level) {
        super(type, level);
    }

    public IceBoltProjectile(Level level, LivingEntity owner, float damage) {
        super(RagnarEntities.ICE_BOLT_PROJECTILE.get(), level, owner, damage);
    }

    @Override
    public void trailParticles() {
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ITEM_SNOWBALL, getX(), getY(), getZ(), 0, 0.05, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 0.2, getY(), getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 20, 0.3, 0.3, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 10, 0.2, 0.2, 0.2, 0.05);
            level().playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    @Override
    public float getSpeed() {
        return 1.2f;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.of(SoundEvents.GLASS_BREAK);
    }
}
