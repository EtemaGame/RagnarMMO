package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RuwachSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "ruwach");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Ruwach: Reveals hidden enemies and deals Holy damage equal to 145% MATK.
        // For Minecraft, we apply a Glowing effect to nearby entities and deal magic
        // damage.

        double radius = 5.0; // RO is 5x5 cells -> approx 5 blocks
        AABB area = player.getBoundingBox().inflate(radius);

        List<Entity> nearby = player.level().getEntities(player, area, e -> e instanceof LivingEntity && e != player);

        float baseDamage = 8.0f; // Roughly 145% multiplier of a base MATK

        for (Entity e : nearby) {
            LivingEntity target = (LivingEntity) e;

            // Damage enemy
            target.hurt(player.damageSources().indirectMagic(null, player), baseDamage);

            // Apply Glowing (revealing effect) for 10 seconds
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
        }

        // VFX and SFX
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f, 1.0f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Circle of light particles
            for (int i = 0; i < 360; i += 20) {
                double x = player.getX() + Math.cos(Math.toRadians(i)) * radius;
                double z = player.getZ() + Math.sin(Math.toRadians(i)) * radius;
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, player.getY() + 0.5, z, 1, 0, 0, 0, 0);
            }
        }
    }
}
