package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Pneuma — Active (Acolyte)
 * RO: Creates a 3x3 cell safety zone that blocks ranged attacks.
 * MC: Grants a temporary "projectile shield" by storing an expiry timestamp
 *     in PersistentData. A Forge SkillEvents hook cancels the next incoming
 *     projectile (arrow, fireball, snowball, etc.) while the shield is active.
 *     Duration: (5 + level * 2) seconds.
 */
public class PneumaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "pneuma");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int durationSec = 5 + level * 2;
        long expiry = player.level().getGameTime() + (long) durationSec * 20;
        player.getPersistentData().putLong("pneuma_expiry", expiry);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.8, 0.5, 0.05);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.3f);
        }

        player.sendSystemMessage(Component.literal(
                "§e✦ Pneuma §factive — Escudo de proyectiles " + durationSec + "s."));
    }
}
