package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.entity.aoe.NapalmBeatAoe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Napalm Beat — Active (Ghost property AOE)
 */
public class NapalmBeatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "napalm_beat");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // RO: Napalm Beat deals (70 + 10 * level)% MATK to all enemies in the area
        float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 70.0f + (level * 10.0f));
        float radius = 2.0f + (level * 0.1f);

        LivingEntity target = getTarget(player);
        Vec3 strikePos;

        if (target != null) {
            strikePos = target.position();
        } else {
            // Raytrace if no target
            HitResult ray = player.pick(10.0D, 0.0F, false);
            strikePos = ray.getLocation();
        }

        // Initial Casting Phase (10 ticks)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    double radius_circle = 1.0;
                    for (int j = 0; j < 8; j++) {
                        double angle = (tick * 0.5) + (j * Math.PI * 2 / 8.0);
                        double dx = Math.cos(angle) * radius_circle;
                        double dz = Math.sin(angle) * radius_circle;
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, player.getX() + dx, player.getY() + 0.1, player.getZ() + dz, 1, 0, 0, 0, 0);
                    }
                }
            });
        }

        // Spawn Napalm Beat AOE entity with delay
        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(10, () -> {
            NapalmBeatAoe aoe = new NapalmBeatAoe(player.level(), player, radius, damage, 40); // 2 second duration
            aoe.setPos(strikePos.x, strikePos.y, strikePos.z);
            player.level().addFreshEntity(aoe);
        });
    }

    private LivingEntity getTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(10.0));

        AABB searchBox = player.getBoundingBox().inflate(10.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(0.5);
            if (targetBox.clip(start, end).isPresent()) {
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
