package com.etema.ragnarmmo.entity.aoe;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireWallAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = new ResourceLocation("ragnarmmo", "fire_wall");

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }
    
    private int hitsLeft = 15; // RO: Fire wall vanishes after some hits

    public FireWallAoe(EntityType<? extends FireWallAoe> type, Level level) {
        super(type, level);
    }

    public FireWallAoe(Level level, LivingEntity owner, float radius, float damage, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.FIRE_WALL_AOE.get(), level, owner, radius, damage, duration);
        this.reapplicationDelay = 2; // Hit very fast
    }

    @Override
    public void applyEffect(LivingEntity target) {
        if (hitsLeft > 0) {
            target.hurt(this.damageSources().inFire(), this.damage);
            Vec3 knockback = target.position().subtract(this.position()).normalize().scale(0.5);
            target.push(knockback.x, 0.1, knockback.z);
            
            hitsLeft--;
            if (hitsLeft <= 0) {
                discard();
            }
            
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
    }

    @Override
    public void ambientParticles() {
        if (level() instanceof ServerLevel sl) {
            for (int i = 0; i < 5; i++) {
                sl.sendParticles(ParticleTypes.FLAME, getX() + (random.nextDouble()-0.5)*0.5, getY() + i * 0.4, getZ() + (random.nextDouble()-0.5)*0.5, 2, 0.1, 0.1, 0.1, 0.02);
            }
            if (tickCount % 10 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }
    }
}
