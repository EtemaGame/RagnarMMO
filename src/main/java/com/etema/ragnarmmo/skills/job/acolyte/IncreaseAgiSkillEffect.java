package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class IncreaseAgiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "increase_agi");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = AcolyteTargetingHelper.resolveSupportTarget(player, 6.0);
        var defOpt = SkillRegistry.get(ID);
        int durationTicks = defOpt
                .map(def -> def.getLevelInt("duration_ticks", level, (40 + 20 * level) * 20))
                .orElse((40 + 20 * level) * 20);
        int amplifier = defOpt
                .map(def -> def.getLevelInt("effect_amplifier", level, level + 1))
                .orElse(level + 1);
        float hpCost = defOpt
                .map(def -> (float) def.getLevelDouble("hp_cost", level, 15.0))
                .orElse(15.0f);

        // Initial Casting Phase (Magic Circle)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    double radius_circle = 1.0;
                    for (int j = 0; j < 8; j++) {
                        double angle = (tick * 0.5) + (j * Math.PI * 2 / 8.0);
                        double dx = Math.cos(angle) * radius_circle;
                        double dz = Math.sin(angle) * radius_circle;
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX() + dx, player.getY() + 0.1, player.getZ() + dz, 1, 0, 0, 0, 0);
                    }
                    SkillVisualFx.spawnRotatingRing(sl, player.position(), 0.9, 0.8, ParticleTypes.CLOUD, 8, tick * 0.5);
                }
            });
        }

        com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(10, () -> {
            LivingEntity finalTarget = target.isAlive() ? target : player;
            if (hpCost > 0.0f) {
                player.setHealth(Math.max(1.0f, player.getHealth() - hpCost));
            }
            finalTarget.addEffect(new MobEffectInstance(RagnarMobEffects.INCREASE_AGI.get(), durationTicks, amplifier));

            // SFX
            player.level().playSound(null, finalTarget.getX(), finalTarget.getY(), finalTarget.getZ(),
                    RagnarSounds.INCREASE_AGI.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            // RO Style: High-speed agility aura (rising quickly)
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                for (int i = 0; i < 20; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 0.6;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 0.6;
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, finalTarget.getX() + ox, finalTarget.getY(), finalTarget.getZ() + oz, 1, 0, 0.4, 0, 0.2);
                    sl.sendParticles(ParticleTypes.CLOUD, finalTarget.getX() + ox, finalTarget.getY() + 0.5, finalTarget.getZ() + oz, 1, 0, 0.2, 0, 0.1);
                }
                SkillVisualFx.spawnAuraColumn(sl, finalTarget, ParticleTypes.CLOUD, ParticleTypes.GLOW, 4, 0.8, 1.8);
            }
        });
    }
}
