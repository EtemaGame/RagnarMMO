package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class HammerFallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "hammer_fall");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            double range = 2.5 + (level * 0.5);
            
            // Ground smash dust/shockwave
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 
                    15, range / 2, 0.1, range / 2, 0.02);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 
                    5, range / 3, 0.1, range / 3, 0.0);
            
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 0.6f);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.5f);

            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());

            float stunChance = 0.3f + (level * 0.05f); // 35% to 80%
            int stunDuration = 60 + (level * 5); // 3s to 5.5s
            
            for (LivingEntity target : targets) {
                if (serverLevel.random.nextFloat() < stunChance) {
                    long until = serverLevel.getGameTime() + stunDuration;
                    target.getPersistentData().putLong("ragnarmmo_stunned_until", until);
                    
                    // Visual/Functional lockdown
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunDuration, 10, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, stunDuration, 0, false, false));
                    
                    // Stun particles on target
                    serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(),
                            5, 0.2, 0.1, 0.2, 0);
                }
            }
        }
    }
}
