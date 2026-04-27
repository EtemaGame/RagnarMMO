package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DarkIllusionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:dark_illusion");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Dark Illusion: Rapidly closes the distance to a target and attacks.
        LivingEntity target = getClosestTarget(player, 15.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            // Teleport behind or next to target
            Vec3 targetPos = target.position();
            Vec3 dir = player.position().subtract(targetPos).normalize();
            Vec3 finalPos = targetPos.add(dir.scale(1.5));

            serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5,
                    0.5, 0.5, 0.1);
            player.teleportTo(finalPos.x, finalPos.y, finalPos.z);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                    1.0f, 1.2f);

            // Strike
            target.hurt(player.damageSources().playerAttack(player), 10.0f + (level * 5.0f));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1,
                    0.1, 0.1, 0.1, 0.1);
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
