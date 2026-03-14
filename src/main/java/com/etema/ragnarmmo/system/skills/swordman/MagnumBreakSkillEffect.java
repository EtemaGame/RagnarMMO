package com.etema.ragnarmmo.system.skills.swordman;

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
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

/**
 * Magnum Break — Active (Fire AoE)
 * RO: Deals Fire property physical damage in a 5x5 area with knockback.
 *     Grants +20% Fire property damage bonus for 10 seconds after cast.
 *
 * Minecraft:
 *  - AoE fire damage + knockback (retained from original).
 *  - The +20% fire damage buff is now tracked via PersistentData tag on the player
 *    ("ragnar_magnum_fire_until"), and applied in onOffensiveHurt if active.
 */
public class MagnumBreakSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "magnum_break");
    public static final String FIRE_BUFF_TAG = "ragnar_magnum_fire_until";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        float baseDamage = 3.0f + (level * 1.5f);
        double radius = 4.0;

        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> nearby = player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e != player && e.isAlive());

        for (Entity e : nearby) {
            LivingEntity target = (LivingEntity) e;
            target.hurt(player.damageSources().mobAttack(player), baseDamage);
            target.setSecondsOnFire(3);

            // Knockback away from player
            net.minecraft.world.phys.Vec3 knockDir = target.position().subtract(player.position()).normalize();
            target.knockback(1.0f, -knockDir.x, -knockDir.z);
        }

        // Grant +20% fire damage buff for 10 seconds via PersistentData
        long buffUntil = player.level().getGameTime() + 200L; // 200 ticks = 10s
        player.getPersistentData().putLong(FIRE_BUFF_TAG, buffUntil);

        // --- Sounds & Sounds ---
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Visual expansion: 3 rings of fire
        for (int r = 1; r <= 3; r++) {
            final double currentR = radius * (r / 3.0);
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(r * 2, () -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    // Circle particles
                    for (int i = 0; i < 360; i += 10) {
                        double rad = Math.toRadians(i);
                        double x = player.getX() + Math.cos(rad) * currentR;
                        double z = player.getZ() + Math.sin(rad) * currentR;
                        serverLevel.sendParticles(ParticleTypes.FLAME, x, player.getY() + 0.1, z, 1, 0, 0, 0, 0.02);
                        if (currentR > 2.0) {
                            serverLevel.sendParticles(ParticleTypes.LAVA, x, player.getY() + 0.1, z, 1, 0, 0, 0, 0);
                        }
                    }
                }
            });
        }

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Passive component: applies the +20% fire damage buff if active.
     * Only triggers on offensive attacks.
     */
    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        long fireUntil = player.getPersistentData().getLong(FIRE_BUFF_TAG);
        if (fireUntil <= 0) return;

        if (player.level().getGameTime() >= fireUntil) {
            player.getPersistentData().remove(FIRE_BUFF_TAG);
            return;
        }

        // +20% fire damage bonus while buff is active
        event.setAmount(event.getAmount() * 1.20f);
    }
}
