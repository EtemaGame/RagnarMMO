package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class CloakSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:cloak");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Cloaking: Advanced invisibility that allows movement.
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.2,
                    0.5, 0.2, 0.02);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS,
                    1.0f, 1.0f);

            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200 + (level * 200), 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200 + (level * 200), 0));
        }
    }
}
