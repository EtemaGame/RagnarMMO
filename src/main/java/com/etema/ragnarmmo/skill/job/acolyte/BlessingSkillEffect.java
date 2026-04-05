package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.entity.effect.StatusOverlayEntity;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class BlessingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "blessing");

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
                .map(def -> def.getLevelInt("effect_amplifier", level, level - 1))
                .orElse(level - 1);

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
                    SkillVisualFx.spawnRotatingRing(sl, player.position(), 0.65, 1.1, ParticleTypes.GLOW, 4, -tick * 0.35);
                }
            });
        }

        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(10, () -> {
            // Apply MobEffect (Handles STR, DEX, INT via AttributeModifiers)
            LivingEntity finalTarget = target.isAlive() ? target : player;
            finalTarget.addEffect(new MobEffectInstance(RagnarMobEffects.BLESSING.get(), durationTicks, amplifier));
            finalTarget.removeEffect(MobEffects.CONFUSION);
            finalTarget.removeEffect(MobEffects.BLINDNESS);
            finalTarget.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            finalTarget.removeEffect(MobEffects.WEAKNESS);
            finalTarget.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            finalTarget.removeEffect(com.etema.ragnarmmo.common.init.RagnarMobEffects.FROZEN.get());
            finalTarget.setTicksFrozen(0);
            StatusOverlayEntity.clearForTarget(player.level(), finalTarget);

            // SFX
            player.level().playSound(null, finalTarget.getX(), finalTarget.getY(), finalTarget.getZ(),
                    RagnarSounds.BLESSING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            // RO Style: Blessing cross/particles above head
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                // Rising cross-like particles
                for (int i = 0; i < 15; i++) {
                    double ox = (player.getRandom().nextDouble() - 0.5) * 0.4;
                    double oz = (player.getRandom().nextDouble() - 0.5) * 0.4;
                    sl.sendParticles(ParticleTypes.INSTANT_EFFECT, finalTarget.getX() + ox, finalTarget.getY() + 1.8, finalTarget.getZ() + oz, 1, 0, 0.1, 0, 0.1);
                    sl.sendParticles(ParticleTypes.GLOW, finalTarget.getX() + ox, finalTarget.getY() + 1.8, finalTarget.getZ() + oz, 1, 0, 0.05, 0, 0.05);
                }
                SkillVisualFx.spawnVerticalCross(sl, finalTarget.position(), 0.25, 1.7, 0.28, ParticleTypes.END_ROD, ParticleTypes.GLOW);
            }
        });
    }
}
