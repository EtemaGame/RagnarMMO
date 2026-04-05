package com.etema.ragnarmmo.skill.api;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility for spawning skill-related visual effects.
 */
public final class SkillVisuals {

    private SkillVisuals() {}

    /**
     * Spawns a generic hit flash effect at the given position.
     */
    public static void spawnHitFlash(Level level, Vec3 pos) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.0, pos.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    /**
     * Spawns casting particles around the user.
     */
    public static void spawnCastParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.1, pos.z, 5, 0.4, 0.1, 0.4, 0.05);
    }
}
