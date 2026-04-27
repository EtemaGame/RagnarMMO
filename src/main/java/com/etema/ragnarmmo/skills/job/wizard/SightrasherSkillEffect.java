package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SightrasherSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:sightrasher");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Sightrasher: Shoots fireballs in 8 directions.
        // For Minecraft, we'll do an outward burst of fire damage.

        AABB area = player.getBoundingBox().inflate(4.0);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40,
                    1.0, 0.2, 1.0, 0.1);
            serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 60, 2.0,
                    0.5, 2.0, 0.2);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f,
                    1.5f);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().magic(), 6.0f + (level * 2.0f));
                target.knockback(0.5, player.getX() - target.getX(), player.getZ() - target.getZ());
            }
        }
    }
}
