package com.etema.ragnarmmo.skills.job.novice;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * First Aid - Active (special novice unlock)
 * RO behavior: Consume 3 SP to restore 5 HP.
 * Minecraft adaptation keeps it as a simple instant self-heal.
 */
public class FirstAidSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "first_aid");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getResourceCost(int level, int defaultCost) {
        return 3;
    }

    @Override
    public void execute(LivingEntity entity, int level) {
        if (!(entity instanceof Player player) || level <= 0) return;

        player.heal(5.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7f, 1.5f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    6, 0.3, 0.2, 0.3, 0.01);
        }
    }
}
