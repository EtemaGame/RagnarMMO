package com.etema.ragnarmmo.skills.job.thief;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DetoxifySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "detoxify");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Cures poison. Works on self or targeted ally.

        double range = SkillRegistry.get(ID)
                .map(def -> def.getLevelDouble("range", level, 5.0D))
                .orElse(5.0D);
        LivingEntity target = getTarget(player, range);
        if (target == null) {
            target = player; // Default to self
        }

        if (target.hasEffect(MobEffects.POISON)) {
            target.removeEffect(MobEffects.POISON);
        } else {
            if (target instanceof Player) {
                ((Player) target)
                        .sendSystemMessage(net.minecraft.network.chat.Component.literal("Target is not poisoned."));
            }
            if (target != player) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Target was not poisoned."));
            }
        }

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.4, 0.5, 0.4, 0.1);
        }
    }

    private LivingEntity getTarget(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));

        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
