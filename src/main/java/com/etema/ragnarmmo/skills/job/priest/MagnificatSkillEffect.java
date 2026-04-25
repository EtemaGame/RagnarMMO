package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class MagnificatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "magnificat");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Magnificat: Doubles SP recovery speed.
        if (level <= 0) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                    50, 5.0, 1.0, 5.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), RagnarSounds.MAGNIFICAT.get(), SoundSource.PLAYERS, 1.0f,
                    1.0f);

            int durationTicks = 600 + (level * 200);
            player.addEffect(new MobEffectInstance(RagnarMobEffects.MAGNIFICAT.get(), durationTicks, 0));
        }
    }
}
