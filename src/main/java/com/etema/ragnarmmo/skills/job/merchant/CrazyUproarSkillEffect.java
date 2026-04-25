package com.etema.ragnarmmo.skills.job.merchant;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class CrazyUproarSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "crazy_uproar");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Crazy Uproar: +4 STR for 5 minutes.
        int durationTicks = 6000; // 5 mins
        player.addEffect(new MobEffectInstance(RagnarMobEffects.CRAZY_UPROAR.get(), durationTicks, 0));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.MAGNUM_BREAK.get(), SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2.0, player.getZ(),
                    5, 0.4, 0.2, 0.4, 0.0);
        }
    }
}
