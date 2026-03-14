package com.etema.ragnarmmo.system.skills.mage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Shared raycast utility for Mage skill effects.
 * Avoids duplicating the 15-block raycast logic across every Mage skill file.
 */
public final class MageTargetUtil {

    private MageTargetUtil() {}

    /**
     * Returns the closest LivingEntity along the player's line of sight
     * within {@code range} blocks, or null if none found.
     */
    public static LivingEntity raycast(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox().inflate(range);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(e.getPickRadius() + 0.3).clip(start, end).isPresent())
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().clip(start, end)
                        .map(hit -> start.distanceToSqr(hit))
                        .orElse(Double.MAX_VALUE)))
                .orElse(null);
    }
}
