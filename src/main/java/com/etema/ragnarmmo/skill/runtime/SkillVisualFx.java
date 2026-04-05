package com.etema.ragnarmmo.skill.runtime;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared server-side helpers for RO-inspired spell and skill visuals.
 */
public final class SkillVisualFx {

    private SkillVisualFx() {
    }

    public static void spawnRing(ServerLevel level, Vec3 center, double radius, double yOffset,
            ParticleOptions particle, int points) {
        if (points <= 0) {
            return;
        }

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnRotatingRing(ServerLevel level, Vec3 center, double radius, double yOffset,
            ParticleOptions particle, int points, double rotationRadians) {
        if (points <= 0) {
            return;
        }

        for (int i = 0; i < points; i++) {
            double angle = rotationRadians + ((Math.PI * 2.0 * i) / points);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnVerticalCross(ServerLevel level, Vec3 center, double yOffset, double height,
            double armWidth, ParticleOptions mainParticle, ParticleOptions accentParticle) {
        int steps = Math.max(6, (int) Math.round(height * 10.0));
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            double y = center.y + yOffset + (height * progress);
            level.sendParticles(mainParticle, center.x, y, center.z, 1, 0, 0, 0, 0);

            if (progress >= 0.35 && progress <= 0.65) {
                level.sendParticles(accentParticle, center.x + armWidth, y, center.z, 1, 0, 0, 0, 0);
                level.sendParticles(accentParticle, center.x - armWidth, y, center.z, 1, 0, 0, 0, 0);
                level.sendParticles(accentParticle, center.x, y, center.z + armWidth, 1, 0, 0, 0, 0);
                level.sendParticles(accentParticle, center.x, y, center.z - armWidth, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void spawnFrontArc(ServerLevel level, LivingEntity user, double forwardDistance, double width,
            double yOffset, ParticleOptions mainParticle, ParticleOptions accentParticle, int points) {
        Vec3 forward = user.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-6) {
            return;
        }

        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 center = user.position().add(forward.scale(forwardDistance));

        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 0.5 : i / (double) (points - 1);
            double offset = (progress - 0.5) * width;
            Vec3 pos = center.add(right.scale(offset));
            level.sendParticles(mainParticle, pos.x, pos.y + yOffset, pos.z, 1, 0, 0, 0, 0);

            if (i % 2 == 0) {
                level.sendParticles(accentParticle, pos.x, pos.y + yOffset + 0.15, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void spawnBlockBurst(ServerLevel level, LivingEntity target, BlockState blockState, int count,
            double spreadXZ, double spreadY, double speed) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                target.getX(), target.getY() + (target.getBbHeight() * 0.5), target.getZ(),
                count, spreadXZ, spreadY, spreadXZ, speed);
    }

    public static void spawnAuraColumn(ServerLevel level, LivingEntity target, ParticleOptions mainParticle,
            ParticleOptions accentParticle, int layers, double radius, double height) {
        int safeLayers = Math.max(1, layers);
        for (int layer = 0; layer < safeLayers; layer++) {
            double progress = safeLayers == 1 ? 0.0 : layer / (double) (safeLayers - 1);
            double y = height * progress;
            double currentRadius = Math.max(0.1, radius * (1.0 - (progress * 0.4)));
            spawnRing(level, target.position(), currentRadius, y, mainParticle, 8);
            if (layer % 2 == 0) {
                spawnRing(level, target.position(), Math.max(0.08, currentRadius * 0.65), y + 0.08, accentParticle, 4);
            }
        }
    }
}
