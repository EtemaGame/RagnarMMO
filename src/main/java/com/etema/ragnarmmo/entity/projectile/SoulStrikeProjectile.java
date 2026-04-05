package com.etema.ragnarmmo.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SoulStrikeProjectile extends AbstractMagicProjectile {

    public SoulStrikeProjectile(EntityType<? extends SoulStrikeProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public SoulStrikeProjectile(Level level, LivingEntity owner, float damage) {
        this(com.etema.ragnarmmo.common.init.RagnarEntities.SOUL_STRIKE_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.damage = damage;
    }

    @Override
    public float getSpeed() {
        return 1.2f;
    }

    @Override
    public Optional<SoundEvent> getImpactSound() {
        return Optional.of(SoundEvents.SOUL_ESCAPE);
    }

    @Override
    public void trailParticles() {
        if (level().isClientSide) {
            double time = tickCount * 0.4;
            double offsetX = Math.sin(time) * 0.2;
            double offsetY = Math.cos(time) * 0.2;
            
            level().addParticle(ParticleTypes.SOUL, 
                getX() + offsetX, getY() + 0.25 + offsetY, getZ(), 
                0, 0.02, 0);
            
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.WITCH, 
                    getX() - offsetX, getY() + 0.25 - offsetY, getZ(), 
                    0, 0, 0);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 15, 0.2, 0.2, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.3, 0.3, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.WITCH, x, y, z, 10, 0.4, 0.4, 0.4, 0.1);
        }
    }
}
