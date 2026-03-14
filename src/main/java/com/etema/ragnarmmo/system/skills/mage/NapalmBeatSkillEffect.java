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
 * Napalm Beat — Active (Ghost/Shadow property)
 * RO: Hits a 3x3 area centred on target with Ghost damage. 3 hits.
 *     Requires a target enemy to centre the AoE.
 *
 * Minecraft:
 *  - Raycast to find target, then 3x3 radius AoE around that target.
 *  - Applies ghost/void magic particles (soul-flame aesthetic).
 *  - Deals magic damage to everything in radius.
 */
public class NapalmBeatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "napalm_beat");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity primary = MageTargetUtil.raycast(player, 12.0);
        if (primary == null) return;

        double radius = 2.5;
        float damagePerHit = (2.0f + (level * 1.0f)); //conceptually 3 hits

        for (int i = 0; i < 3; i++) {
            final int hitIndex = i;
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(i * 2, () -> {
                if (!primary.isAlive() && hitIndex == 0) return; // Stop if primary died before first hit

                AABB area = primary.getBoundingBox().inflate(radius);
                List<Entity> nearby = primary.level().getEntities(primary, area,
                        e -> e instanceof LivingEntity && e != player && e.isAlive());
                nearby.add(primary);

                for (Entity e : nearby) {
                    ((LivingEntity) e).hurt(player.damageSources().indirectMagic(null, player), damagePerHit);
                }

                player.level().playSound(null, primary.getX(), primary.getY(), primary.getZ(),
                        SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.9f, 1.4f + (hitIndex * 0.1f));

                if (player.level() instanceof ServerLevel sl) {
                    // Central spectral burst
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            primary.getX(), primary.getY() + 1, primary.getZ(),
                            20, 0.5, 0.5, 0.5, 0.05);
                    // Area smoke
                    sl.sendParticles(ParticleTypes.SMOKE,
                            primary.getX(), primary.getY() + 1, primary.getZ(),
                            10, radius / 2, 0.3, radius / 2, 0.02);
                }
            });
        }
    }
}
