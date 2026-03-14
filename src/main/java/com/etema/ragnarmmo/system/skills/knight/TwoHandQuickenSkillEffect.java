package com.etema.ragnarmmo.system.skills.knight;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Two-Hand Quicken — Active (2H Sword speed buff)
 * RO: Increases ASPD by 30% for 30+30×level seconds when using 2H Swords.
 *     This is the primary reason Knights use 2H Swords.
 *
 * Minecraft:
 *  - Grants MOVEMENT_SPEED + ATTACK_SPEED attribute boost.
 *  - We use Haste (speeds up attack cooldown recovery) as the closest analog.
 *  - Duration: 30s + 30s per level (600-6600 ticks).
 *  - Only gives Haste (not DIG_SPEED — that was a bug in the old code).
 */
public class TwoHandQuickenSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "two_hand_quicken");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Duration: 30s + 30s/level
        int durationTicks = (30 + 30 * level) * 20;

        // Haste amplifier: scales from 0 (Haste 1) to 2 (Haste 3) based on level
        int hasteAmplifier = Math.min(2, (level - 1) / 3); // 0 at lv1-3, 1 at lv4-6, 2 at lv7-10

        // Haste = faster attack cooldown recovery (closest to ASPD in Minecraft)
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, hasteAmplifier, false, true, true));

        // Minor Speed boost to represent the combat flow of Two-Hand Quicken
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 0, false, true, true));

        // --- Particles & Sound ---
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.4, 0.7, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    8, 0.2, 0.5, 0.2, 0.05);
        }
    }
}
