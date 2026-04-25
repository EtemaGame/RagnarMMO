package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.common.init.RagnarSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SightMobEffect extends MobEffect {
    public static final String CLOAKED_TAG = "ragnar_cloaked_until";

    public SightMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF4500); // Orange/Fire color
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        ServerLevel world = (ServerLevel) entity.level();

        // 1. Particle Effect: Spinning Fireball
        double radius = 1.2;
        // Faster spin for more impact: 2 full rotations per second (40 ticks)
        double angle = (entity.tickCount * 0.3); 
        
        double px = entity.getX() + Math.cos(angle) * radius;
        double py = entity.getY() + 1.2 + Math.sin(entity.tickCount * 0.1) * 0.2; // Slight bobbing
        double pz = entity.getZ() + Math.sin(angle) * radius;

        // Core of the fireball
        world.sendParticles(ParticleTypes.FLAME, px, py, pz, 3, 0.05, 0.05, 0.05, 0.02);
        if (entity.tickCount % 2 == 0) {
            world.sendParticles(ParticleTypes.SMALL_FLAME, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
        }
        
        // Occasional smoke trail
        if (entity.tickCount % 5 == 0) {
            world.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0, 0, 0.01);
        }

        // 2. Detection Logic (7x7 area = radius 3.5 blocks)
        if (entity.tickCount % 10 == 0) {
            AABB area = entity.getBoundingBox().inflate(3.5);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != entity && e.isAlive());

            boolean foundAny = false;
            for (LivingEntity target : targets) {
                boolean wasHidden = false;
                
                // Reveal if it has the custom cloaked tag
                if (target.getPersistentData().contains(CLOAKED_TAG)) {
                    target.getPersistentData().remove(CLOAKED_TAG);
                    target.setInvisible(false);
                    wasHidden = true;
                }
                
                // Also handle vanilla invisibility if necessary, or just apply glowing to everything hidden
                if (target.isInvisible() || wasHidden) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
                    foundAny = true;
                }
            }
            
            if (foundAny) {
                // Play a subtle "reveal" sound or just the sight sound again?
                // For now, let's keep it quiet to focus on the fireball.
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Tick every single tick for smooth animation
    }
}
