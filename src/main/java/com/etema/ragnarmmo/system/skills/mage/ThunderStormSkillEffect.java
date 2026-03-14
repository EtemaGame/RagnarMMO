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

import java.util.List;

/**
 * Thunder Storm — Active (Wind/Lightning AoE)
 * RO: Hits a 5×5 area multiple times with Wind property magic damage.
 *     Deals damage to all targets around the cast location each second.
 *
 * Minecraft:
 *  - AoE centred on the player, radius scales with level.
 *  - Each target is struck with lightning particles + thunder sound.
 *  - Deals magic (wind) damage. Uses ELECTRIC_SPARK + FLASH particles.
 */
public class ThunderStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "thunder_storm");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double radius = 3.0 + level * 0.5; // grows with level up to 8 blocks at lv10
        float damage = 3.0f + (level * 1.5f);

        AABB area = player.getBoundingBox().inflate(radius);
        List<Entity> nearby = player.level().getEntities(player, area,
                e -> e instanceof LivingEntity && e != player && e.isAlive());

        for (Entity e : nearby) {
            LivingEntity target = (LivingEntity) e;
            target.hurt(player.damageSources().indirectMagic(null, player), damage);
        }

        // Sounds: thunder crack + lightning impact
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player.level() instanceof ServerLevel sl) {
            // Lightning bolt columns at random positions within radius
            for (int i = 0; i < Math.min(level, 8); i++) {
                double ox = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                double oz = (player.getRandom().nextDouble() - 0.5) * radius * 2;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + ox, player.getY() + 2, player.getZ() + oz,
                        15, 0.1, 0.8, 0.1, 0.05);
                sl.sendParticles(ParticleTypes.FLASH,
                        player.getX() + ox, player.getY() + 0.5, player.getZ() + oz,
                        1, 0, 0, 0, 0);
            }
            // Wide area electric glitter
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1, player.getZ(),
                    100, radius / 2, 1.0, radius / 2, 0.15);
        }
    }
}
