package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SoulStrikeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "soul_strike");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getTarget(player);
        // Allow shooting even if target is null

        int hits = (level + 1) / 2; // RO: 1-5 hits based on level
        // RO: Soul Strike deals 100% MATK per hit.
        float rawDamage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 100.0f);

        if (target != null && target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            // RO: deals extra damage to undead (approx 5% per level in some versions, or just holy property)
            rawDamage *= (1.0f + (level * 0.05f));
        }

        final float finalDamage = rawDamage;
        Vec3 start = player.getEyePosition().subtract(0, 0.3, 0);
        
        // Initial Casting Phase (10 ticks of particles)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    // RO Style: Magic Circle simulation
                    double radius = 1.0;
                    for (int j = 0; j < 8; j++) {
                        double angle = (tick * 0.5) + (j * Math.PI * 2 / 8.0);
                        double dx = Math.cos(angle) * radius;
                        double dz = Math.sin(angle) * radius;
                        sl.sendParticles(ParticleTypes.SOUL, player.getX() + dx, player.getY() + 0.1, player.getZ() + dz, 1, 0, 0, 0, 0);
                    }
                    
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 8, 0.5, 0.1, 0.5, 0.05);
                    sl.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        for (int i = 0; i < hits; i++) {
            // Projectiles start after 10 ticks, then 3 ticks apart
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(10 + i * 3, () -> {
                Vec3 currentStart = player.getEyePosition().subtract(0, 0.2, 0);
                Vec3 dir;
                
                if (target != null && target.isAlive()) {
                    Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
                    dir = targetPos.subtract(currentStart).normalize();
                } else {
                    dir = player.getLookAngle();
                }

                com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile projectile = 
                    new com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile(player.level(), player, finalDamage);
                
                projectile.setPos(currentStart.x, currentStart.y, currentStart.z);
                projectile.shoot(dir);
                
                player.level().addFreshEntity(projectile);
            });
        }
    }

    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(12.0));

        AABB searchBox = player.getBoundingBox().inflate(12.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

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
}
