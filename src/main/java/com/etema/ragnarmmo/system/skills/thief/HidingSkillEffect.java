package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class HidingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "hiding");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Hiding: Grants invisibility. Lasts 30s to 300s.
        // Drains SP natively in RO, or we can just give a set duration vanilla
        // Invisibility.
        // It breaks on attacking - we will handle the break in an attack event hook
        // later if needed,
        // but Minecraft inherently handles targeting loss somewhat natively.

        int durationTicks = (30 * level) * 20; // 30s at lv1, 300s at lv10

        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            // Toggle off if already active
            player.removeEffect(MobEffects.INVISIBILITY);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 1.5f);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, false, false, true));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(),
                        20, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }
}
