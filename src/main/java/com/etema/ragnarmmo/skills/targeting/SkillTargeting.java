package com.etema.ragnarmmo.skills.targeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utility for finding and resolving skill targets.
 */
public final class SkillTargeting {

    private SkillTargeting() {}

    /**
     * Finds the closest living entity in the user's line of sight within the given range.
     * For Mobs, it prioritizes their current target if available.
     */
    public static LivingEntity findEntityInSight(LivingEntity user, double range) {
        if (user instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            return mob.getTarget();
        }

        Vec3 start = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = start.add(look.scale(range));

        AABB searchBox = user.getBoundingBox().inflate(range);
        List<LivingEntity> possibleTargets = user.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != user && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent()) {
                double dist = start.distanceToSqr(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }

        return closestTarget;
    }

    /**
     * Resolves the position where a strike should land. 
     * If an entity is in sight, returns its position.
     * Otherwise, returns the position the user is looking at.
     */
    public static Vec3 resolveStrikePosition(LivingEntity user, double range) {
        LivingEntity target = findEntityInSight(user, range);
        if (target != null && target.isAlive()) {
            return target.position();
        }
        return user.pick(range, 0.0f, false).getLocation();
    }
}
