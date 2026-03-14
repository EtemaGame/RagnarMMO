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

public class WaterBallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:water_ball");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Water Ball: Shoots multiple water spheres if near water.
        // For Minecraft, we'll check if player is in water or it's raining.

        boolean nearWater = player.isInWater() || player.level().isRainingAt(player.blockPosition());
        if (!nearWater)
            return;

        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            int balls = 1 + (level * 2);
            for (int i = 0; i < balls; i++) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1.0, target.getZ(), 10,
                        0.3, 0.3, 0.3, 0.1);
                target.hurt(player.damageSources().indirectMagic(null, player), 2.0f + (level * 0.5f));
            }
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f,
                    1.0f);
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
