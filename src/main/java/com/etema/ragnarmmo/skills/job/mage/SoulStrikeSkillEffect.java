package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SoulStrikeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "soul_strike");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getCastTime(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_time_ticks", level, 24))
                .orElse(24);
    }

    @Override
    public int getCastDelay(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_delay_ticks", level, 10))
                .orElse(10);
    }

    @Override
    public int getResourceCost(int level, int defaultCost) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("sp_cost", level, defaultCost))
                .orElse(defaultCost);
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        var definition = SkillRegistry.require(ID);
        LivingEntity target = getTarget(player, definition.getLevelDouble("range", level, 12.0D));
        // Allow shooting even if target is null

        int hits = definition.getLevelInt("hit_count", level, (level + 1) / 2);
        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 100.0D);
        float rawDamage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, damagePercent);

        if (target != null && target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            rawDamage *= (float) definition.getLevelDouble("undead_multiplier", level, 1.0D + (level * 0.05D));
        }

        final float finalDamage = rawDamage;
        Vec3 start = player.getEyePosition().subtract(0, 0.3, 0);
        
        // Initial Casting Phase (10 ticks of particles)
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(t, () -> {
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
            com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(i * 3, () -> {
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
