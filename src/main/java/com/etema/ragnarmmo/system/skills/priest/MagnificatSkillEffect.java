package com.etema.ragnarmmo.system.skills.priest;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class MagnificatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:magnificat");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Magnificat: Doubles SP recovery speed for the party.
        // For Minecraft, we'll apply a Regeneration-like effect for SP or Haste.

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                    50, 5.0, 1.0, 5.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f,
                    1.5f);

            // Party logic should be handled here, for now player only.
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600 + (level * 200), 1));
        }
    }
}
