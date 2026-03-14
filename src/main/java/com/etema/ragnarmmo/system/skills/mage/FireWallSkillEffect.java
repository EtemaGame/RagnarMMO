package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fire Wall — Active (Ground trap, Fire property)
 * RO: Places a wall of fire in front of the caster. Enemies walking through
 *     take multiple hits of fire damage. Lasts up to 25 seconds.
 *
 * Minecraft:
 *  - Damages all entities touching the wall in front of the player immediately
 *    (instant AoE line, 5 blocks wide, 2 blocks tall).
 *  - Sets targets on fire for duration scaling with level.
 *  - Particle wall visualizes the wall's extent.
 *  - NOTE: True persistent trap behavior would require a block-entity or
 *    a ScheduledTask system. This implementation gives the damaging hit
 *    and leaves a visual effect — a future enhancement can add the persistent
 *    trap via a TemporalWallManager.
 */
public class FireWallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_wall");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        Vec3 forward = player.getLookAngle().multiply(1, 0, 1).normalize(); // Horizontal only
        Vec3 right = new Vec3(-forward.z, 0, forward.x); // Perpendicular

        // Wall center: 2 blocks in front of player
        Vec3 wallCenter = player.position().add(0, 1, 0).add(forward.scale(2.5));
        int wallWidth = 2 + (level / 3); // 2-5 blocks wide depending on level

        float damage = 3.0f + (level * 1.0f);
        int fireTicks = (10 + level * 3) * 20; // 13s to 40s

        for (int i = -wallWidth; i <= wallWidth; i++) {
            Vec3 wallPos = wallCenter.add(right.scale(i));
            AABB slab = new AABB(wallPos.x - 0.5, wallPos.y - 1, wallPos.z - 0.5,
                                  wallPos.x + 0.5, wallPos.y + 2, wallPos.z + 0.5);

            List<Entity> hits = player.level().getEntities(player, slab,
                    e -> e instanceof LivingEntity && e != player && e.isAlive());

            for (Entity e : hits) {
                LivingEntity t = (LivingEntity) e;
                t.hurt(player.damageSources().inFire(), damage);
                t.setRemainingFireTicks((int) (fireTicks / 20f)); // setSecondsOnFire
                t.setSecondsOnFire(fireTicks / 20);
            }

            // Particle column at each wall segment
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME,
                        wallPos.x, wallPos.y, wallPos.z, 8, 0.1, 0.8, 0.1, 0.05);
                sl.sendParticles(ParticleTypes.LAVA,
                        wallPos.x, wallPos.y + 0.5, wallPos.z, 2, 0.05, 0.3, 0.05, 0.0);
            }
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
