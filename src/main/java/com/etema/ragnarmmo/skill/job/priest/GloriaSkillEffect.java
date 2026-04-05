package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class GloriaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:gloria");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Gloria: Increases LUK (+30) for the party.
        // For Minecraft, we'll apply Luck and maybe Hero of the Village.

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 50, 5.0,
                    1.0, 5.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.0f, 2.0f);

            int durationTicks = (7 + level * 3) * 20; // RO: 7 + 3*level seconds.
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, durationTicks, 2));
        }
    }
}
