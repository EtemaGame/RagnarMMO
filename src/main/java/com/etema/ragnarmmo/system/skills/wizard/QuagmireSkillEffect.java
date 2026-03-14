package com.etema.ragnarmmo.system.skills.wizard;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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

public class QuagmireSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:quagmire");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Quagmire: Creates a swamp area that slows and reduces AGI/DEX.
        AABB area = player.getBoundingBox().inflate(5.0);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY(), player.getZ(), 50, 4.0,
                    0.1, 4.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.MUD_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2 + level / 2));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, level / 3));
            }
        }
    }
}
