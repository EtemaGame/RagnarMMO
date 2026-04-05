package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Sight — Active
 * RO: Reveals all hidden/cloaked enemies in a 7x7 area for 10 seconds.
 * Each cast consumes 10 SP.
 */
public class SightSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "sight");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Apply the duration-based SIGHT effect (200 ticks = 10 seconds)
        player.addEffect(new MobEffectInstance(RagnarMobEffects.SIGHT.get(), 200, level - 1, false, false, true));
        
        // Play the specialized Sight sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.SIGHT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
