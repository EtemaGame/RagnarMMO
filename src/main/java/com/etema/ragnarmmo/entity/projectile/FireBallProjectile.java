package com.etema.ragnarmmo.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.etema.ragnarmmo.common.init.RagnarEntities;

/**
 * Projectile for the Fire Ball skill.
 */
public class FireBallProjectile extends AbstractMagicProjectile {

    public FireBallProjectile(EntityType<? extends FireBallProjectile> type, Level level) {
        super(type, level);
    }

    public FireBallProjectile(Level level, LivingEntity owner, float damage) {
        super(RagnarEntities.MAGIC_PROJECTILE.get(), level, owner, damage);
        this.setProjectileType("fireball");
    }

    @Override
    public void trailParticles() {
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0.05, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }
}
