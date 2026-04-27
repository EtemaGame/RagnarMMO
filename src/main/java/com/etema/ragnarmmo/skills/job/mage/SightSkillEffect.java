package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sight");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int durationTicks = SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("duration_ticks", level, 200))
                .orElse(200);
        player.addEffect(new MobEffectInstance(RagnarMobEffects.SIGHT.get(), durationTicks, level - 1, false, false, true));
        
        // Play the specialized Sight sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.SIGHT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
