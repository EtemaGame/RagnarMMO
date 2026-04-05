package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class GloriaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "gloria");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Gloria: Increases LUK (+3 per level, +30 at lv.10) for the party.
        if (level <= 0) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 50, 5.0,
                    1.0, 5.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), RagnarSounds.GLORIA.get(), SoundSource.PLAYERS,
                    1.0f, 1.0f);

            int durationTicks = (7 + level * 3) * 20; // RO: 7 + 3*level seconds.
            // Apply GLORIA MobEffect (Handles +3 LUK per level via AttributeModifier)
            player.addEffect(new MobEffectInstance(RagnarMobEffects.GLORIA.get(), durationTicks, level - 1));
        }
    }
}
