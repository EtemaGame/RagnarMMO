package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * One-Hand Quicken — Active (1H Sword/Dagger speed buff)
 * RO: Increases ASPD by 30% for 30+30×level seconds when using 1H weapons.
 *
 * Minecraft:
 *  - Same mechanic as Two-Hand Quicken but does NOT grant the speed bonus.
 *  - Haste scales slightly slower (1H is already fast in RO, so the buff is less dramatic).
 */
public class OneHandQuickenSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "one_hand_quicken");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int durationTicks = (30 + 30 * level) * 20;
        int hasteAmplifier = Math.min(1, (level - 1) / 5); // 0 at lv1-5, 1 at lv6-10

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, hasteAmplifier, false, true, true));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.6f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.3, 0.6, 0.3, 0.1);
        }
    }
}
