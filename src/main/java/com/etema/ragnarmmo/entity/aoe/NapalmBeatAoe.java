package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class NapalmBeatAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "napalm_beat");

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    public NapalmBeatAoe(EntityType<? extends NapalmBeatAoe> type, Level level) {
        super(type, level);
        this.duration = 20; // Short duration for a "hit"
        this.reapplicationDelay = 5;
    }

    public NapalmBeatAoe(Level level, LivingEntity owner, float radius, float damage, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.NAPALM_BEAT_AOE.get(), level, owner, radius, damage, duration);
    }

    @Override
    public void ambientParticles() {
        if (tickCount == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 30, 0.4, 0.4, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 20, 0.5, 0.5, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 0.5, getZ(), 15, 0.8, 0.1, 0.8, 0.02);
            
            level().playSound(null, getX(), getY(), getZ(), 
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 
                0.8f, 0.6f);
        }

        if (tickCount % 2 == 0) {
            float r = getRadius();
            for (int i = 0; i < 5; i++) {
                double x = getX() + (random.nextDouble() - 0.5) * r * 2;
                double y = getY() + 0.5;
                double z = getZ() + (random.nextDouble() - 0.5) * r * 2;
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.02, 0);
                
                if (random.nextFloat() < 0.3f) {
                    level().addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0.01, 0);
                }
            }
        }
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
