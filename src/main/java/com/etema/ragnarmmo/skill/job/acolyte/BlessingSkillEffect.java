package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BlessingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "blessing");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Blessing: Temporarily increases STR, INT, and DEX of the target by 1 per
        // level.
        // Duration: 60 + 20 * level seconds.
        // For Minecraft, we'll apply it to the player who cast it, or their immediate
        // look target.
        // Since we don't have a robust cross-entity temporary buff system built in
        // here,
        // we will apply it as a bonus to the caster for simplicity, or we can use
        // attributes directly if we want.

        int bonusAmount = level; // RO: +1 per Skill Level to STR, INT, DEX.
        int durationTicks = (60 + 20 * level) * 20; // RO: 60 + 20*level seconds.

        // Apply to self
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
            // Apply bonus (placeholder logic as it was)
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                stats.addBonus(StatKeys.STR, bonusAmount);
                stats.addBonus(StatKeys.DEX, bonusAmount);
                stats.addBonus(StatKeys.INT, bonusAmount);
            });

            // SFX
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 2.0f);

            // RO Style: Blessing cross/particles above head
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                // Rising cross-like particles
                for (int i = 0; i < 15; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 0.4;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 0.4;
                    sl.sendParticles(ParticleTypes.INSTANT_EFFECT, player.getX() + ox, player.getY() + 1.8, player.getZ() + oz, 1, 0, 0.1, 0, 0.1);
                    sl.sendParticles(ParticleTypes.GLOW, player.getX() + ox, player.getY() + 1.8, player.getZ() + oz, 1, 0, 0.05, 0, 0.05);
                }
            }
        });
    }
}
