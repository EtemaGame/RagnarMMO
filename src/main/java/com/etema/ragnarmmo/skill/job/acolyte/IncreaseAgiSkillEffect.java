package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class IncreaseAgiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "increase_agi");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Increase AGI: Temporarily increases AGI by (2 + level) and increases movement
        // speed.
        // Duration: 40 + 20 * level seconds.
        // Applied to self for now.

        int agiBonus = level; // RO: +1 per Skill Level.
        int durationTicks = (40 + 20 * level) * 20; // RO: 40 + 20*level seconds.
        int speedLevel = level >= 6 ? 1 : 0;

        // Initial Casting Phase (Magic Circle)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    double radius_circle = 1.0;
                    for (int j = 0; j < 8; j++) {
                        double angle = (tick * 0.5) + (j * Math.PI * 2 / 8.0);
                        double dx = Math.cos(angle) * radius_circle;
                        double dz = Math.sin(angle) * radius_circle;
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX() + dx, player.getY() + 0.1, player.getZ() + dz, 1, 0, 0, 0, 0);
                    }
                }
            });
        }

        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(10, () -> {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                stats.addBonus(StatKeys.AGI, agiBonus);
            });

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, speedLevel, false, false, true));

            // SFX
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);

            // RO Style: High-speed agility aura (rising quickly)
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                for (int i = 0; i < 20; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 0.6;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 0.6;
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX() + ox, player.getY(), player.getZ() + oz, 1, 0, 0.4, 0, 0.2);
                    sl.sendParticles(ParticleTypes.CLOUD, player.getX() + ox, player.getY() + 0.5, player.getZ() + oz, 1, 0, 0.2, 0, 0.1);
                }
            }
        });
    }
}
