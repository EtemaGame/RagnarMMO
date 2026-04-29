package com.etema.ragnarmmo.entity.aoe;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class StormGustAoe extends AoeEntity {
    private static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "storm_gust");

    @Override
    public ResourceLocation getSkillId() {
        return SKILL_ID;
    }

    public StormGustAoe(EntityType<? extends StormGustAoe> type, Level level) {
        super(type, level);
    }

    public StormGustAoe(Level level, LivingEntity owner, float radius, float damage, int duration) {
        super(com.etema.ragnarmmo.common.init.RagnarEntities.STORM_GUST_AOE.get(), level, owner, radius, damage, duration);
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    @Override
    public void ambientParticles() {
        if (level() instanceof ServerLevel sl) {
            float r = getRadius();
            double angle = tickCount * 0.5;
            for (int i = 0; i < 3; i++) {
                double currentAngle = angle + (i * Math.PI * 2 / 3.0);
                double px = getX() + Math.cos(currentAngle) * r;
                double pz = getZ() + Math.sin(currentAngle) * r;
                
                sl.sendParticles(ParticleTypes.SNOWFLAKE, px, getY() + 0.5 + random.nextDouble(), pz, 5, 0.2, 0.5, 0.2, 0.05);
                sl.sendParticles(ParticleTypes.CLOUD, px, getY() + 0.2, pz, 1, 0.1, 0.1, 0.1, 0.02);
            }
            
            if (tickCount % 20 == 0) {
                level().playSound(null, getX(), getY(), getZ(), 
                        SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        }
    }
}
