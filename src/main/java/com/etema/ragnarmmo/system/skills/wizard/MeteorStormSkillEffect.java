package com.etema.ragnarmmo.system.skills.wizard;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

public class MeteorStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "meteor_storm");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Meteor Storm: Multiple meteors fall in an area.
        // For Minecraft, we'll simulate this with multiple delayed bursts or
        // a large area effect with multiple hits.

        AABB area = player.getBoundingBox().inflate(8.0);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                    1.0f, 0.5f);

            // Number of meteors = level (simplified)
            for (int i = 0; i < level; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 16.0;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 16.0;
                double x = player.getX() + offsetX;
                double z = player.getZ() + offsetZ;
                double y = player.getY();

                serverLevel.sendParticles(ParticleTypes.LAVA, x, y + 1.0, z, 20, 0.5, 0.5, 0.5, 0.1);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y + 1.0, z, 1, 0, 0, 0, 0);

                AABB hitArea = new AABB(x - 1.5, y - 1.0, z - 1.5, x + 1.5, y + 2.0, z + 1.5);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitArea,
                        e -> e != player && e.isAlive());
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().indirectMagic(null, player), 8.0f + (level * 1.5f));
                    if (RANDOM.nextFloat() < 0.3f) { // Stun chance (RO mechanics)
                        // Apply stun if the mod has it, for now slow + fire
                        target.setRemainingFireTicks(60);
                    }
                }
            }
        }
    }
}
