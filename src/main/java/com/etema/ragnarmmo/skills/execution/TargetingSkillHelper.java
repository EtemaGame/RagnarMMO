package com.etema.ragnarmmo.skills.execution;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class TargetingSkillHelper {
    private TargetingSkillHelper() {
    }

    @Nullable
    public static LivingEntity raycast(ServerPlayer player, double range, Predicate<LivingEntity> filter) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox().inflate(range);

        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != player && entity.isAlive() && filter.test(entity));

        for (LivingEntity entity : candidates) {
            AABB targetBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.35D);
            var hit = targetBox.clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = entity;
                }
            }
        }

        return closestTarget;
    }

    public static Vec3 targetPoint(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 0.0F, false);
        return hit.getLocation();
    }

    public static List<LivingEntity> livingAround(ServerPlayer player, Vec3 center, double radius, double verticalRange,
            Predicate<LivingEntity> filter) {
        AABB area = new AABB(
                center.x - radius, center.y - verticalRange, center.z - radius,
                center.x + radius, center.y + verticalRange, center.z + radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && filter.test(entity));
    }

    public static boolean isHostileTo(ServerPlayer player, LivingEntity entity) {
        if (!entity.isAlive() || entity == player) {
            return false;
        }
        if (entity instanceof Enemy) {
            return true;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
            return false;
        }
        if (entity instanceof NeutralMob neutralMob) {
            return neutralMob.getTarget() == player || neutralMob.isAngryAt(player);
        }
        return entity instanceof Mob mob && mob.getTarget() == player;
    }
}
