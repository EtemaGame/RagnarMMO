package com.etema.ragnarmmo.skills.job.swordman;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class MagnumBreakFireMobEffect extends MobEffect {
    public MagnumBreakFireMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6B1A);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double radius = 0.7 + (amplifier * 0.1);
        double angle = (entity.tickCount * 0.35) % (Math.PI * 2.0);
        double x = entity.getX() + Math.cos(angle) * radius;
        double y = entity.getY() + 1.0 + Math.sin(entity.tickCount * 0.12) * 0.15;
        double z = entity.getZ() + Math.sin(angle) * radius;

        serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 3, 0.06, 0.05, 0.06, 0.01);
        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.1, z, 1, 0.02, 0.02, 0.02, 0.0);
        if (entity.tickCount % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    2, 0.2, 0.25, 0.2, 0.01);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0 && duration % 4 == 0;
    }
}
