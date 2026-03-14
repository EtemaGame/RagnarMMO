package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HealSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "heal");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Implementation of RO Heal: Heals target (or self), or damages Undead.
        // Base formula approximation: Base Heal = ((BaseLv + INT) / 8) * (4 + 8 *
        // SkillLv)
        // For Minecraft, we simplify: Base Heal = 4.0 + (level * 2.0) + (INT bonus)
        // We need to determine the target.

        LivingEntity target = getTarget(player);

        float healAmount = 4.0f + (level * 2.0f); // Default flat heal

        // Add INT scaling if stats are available
        // player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(...)

        if (target.getMobType() == MobType.UNDEAD) {
            // Damage undead
            target.hurt(player.damageSources().magic(), healAmount);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.ZOMBIE_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
        } else {
            // Heal target or self
            target.heal(healAmount);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F, 1.2F);
        }
    }

    // Quick raycast to find target, defaults to self if no valid target in sight
    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(5.0));

        AABB searchBox = player.getBoundingBox().inflate(5.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius());
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent()) {
                double dist = start.distanceToSqr(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }

        return closestTarget != null ? closestTarget : player;
    }
}
