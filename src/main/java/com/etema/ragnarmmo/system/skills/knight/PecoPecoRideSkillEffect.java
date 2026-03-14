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
 * Peco Peco Ride — Active (Mount skill)
 * RO: Allows the Knight to ride a Peco Peco, greatly increasing movement speed
 *     and enabling Brandish Spear. However, reduces ASPD slightly while mounted.
 *
 * Minecraft:
 *  - All players can ride horses in Minecraft natively, so this skill instead
 *    grants a Speed buff simulating the mounted movement advantage.
 *  - Also grants temporary Strength 1 (simulating the mounted charge bonus).
 *  - In a future expansion, this could spawn a rideable "Peco Peco" mob entity.
 */
public class PecoPecoRideSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "peco_peco_ride");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Duration: 60 seconds (constant, since in RO this is a toggle)
        int durationTicks = 1200;

        // Speed boost: simulates Peco Peco sprint (Speed 2 = very visible boost)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 1, false, true, true));

        // Strength 1: simulates mounted charge damage advantage
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 0, false, true, true));

        // Jump Boost: mounted entities can leap more effectively
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, 1, false, true, true));

        // --- Feedback ---
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 1.0f, 0.9f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    8, 0.4, 0.1, 0.4, 0.05);
        }
    }
}
