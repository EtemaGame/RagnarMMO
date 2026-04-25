package com.etema.ragnarmmo.skills.job.thief;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.etema.ragnarmmo.mobs.util.MobUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public class HidingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "hiding");

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

        var definition = SkillRegistry.require(ID);
        int durationTicks = definition.getLevelInt("duration_ticks", level, (30 * level) * 20);

        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            // Toggle off if already active
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 1.5f);
        } else {
            // Add full invisibility and scaling Slowness
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, false, false, true));
            int slownessAmp = definition.getLevelInt("slowness_amplifier", level, Math.max(0, (10 - level) / 3));
            if (slownessAmp >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, slownessAmp, false, false, true));
            }
            
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.4f);

            // Force nearby mobs to lose target
            double revealRadius = definition.getLevelDouble("reveal_radius", level, 32.0D);
            AABB area = player.getBoundingBox().inflate(revealRadius);
            player.level().getEntitiesOfClass(Mob.class, area).forEach(mob -> {
                if (mob.getTarget() == player && !MobUtils.isMVPBoss(mob)) {
                    mob.setTarget(null);
                }
            });

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.5, player.getZ(),
                        30, 0.4, 0.2, 0.4, 0.02);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.1, player.getZ(),
                        10, 0.3, 0.1, 0.3, 0.01);
            }
        }
    }
}
