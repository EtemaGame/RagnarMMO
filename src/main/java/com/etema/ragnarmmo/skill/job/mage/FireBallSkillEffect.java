package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FireBallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "fire_ball");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity primaryTarget = getTarget(player);
        // Removed return if primaryTarget == null to allow shooting in the air

        final double radius = 5.0;
        // RO: Fire Ball deals (70 + (15 * level))% MATK at the impact point (AoE)
        final float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 70.0f + (level * 15.0f));

        // Initial Casting Phase (12 ticks of particles)
        for (int t = 0; t < 12; t++) {
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 5, 0.4, 0.1, 0.4, 0.05);
                    sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.1, player.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(12, () -> {
            Vec3 startPos = player.getEyePosition().subtract(0, 0.3, 0);
            Vec3 shootDir;
            
            if (primaryTarget != null && primaryTarget.isAlive()) {
                Vec3 targetVec = primaryTarget.position().add(0, primaryTarget.getBbHeight() / 2.0, 0);
                shootDir = targetVec.subtract(startPos).normalize();
            } else {
                shootDir = player.getLookAngle();
            }

            com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, damage, ParticleTypes.FLAME);
            
            projectile.setSecondaryParticle(ParticleTypes.SMOKE);
            projectile.setProjectileType("fireball");
            projectile.setHoming(false); // Ballistic physics
            projectile.setGravity(0.0f); // RO Fireball goes in a straight line
            
            projectile.setPos(startPos.x, startPos.y, startPos.z);
            projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.2f, 0.0f); // Faster, straight line
            
            projectile.setOnHitEffect(result -> {
                // Impact AOE Damage
                Vec3 hitLoc = result.getLocation();
                AABB area = new AABB(hitLoc.x - radius, hitLoc.y - radius, hitLoc.z - radius,
                                     hitLoc.x + radius, hitLoc.y + radius, hitLoc.z + radius);
                
                List<net.minecraft.world.entity.Entity> nearby = player.level().getEntities(player, area,
                        e -> e instanceof LivingEntity && e != player);

                for (net.minecraft.world.entity.Entity e : nearby) {
                    LivingEntity target = (LivingEntity) e;
                    target.hurt(player.damageSources().indirectMagic(null, player), damage);
                    target.setSecondsOnFire((int) (damage / 2));
                }

                player.level().playSound(null, hitLoc.x, hitLoc.y, hitLoc.z,
                        SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitLoc.x, hitLoc.y, hitLoc.z, 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.FLAME, hitLoc.x, hitLoc.y, hitLoc.z, 40, radius/2, radius/2, radius/2, 0.1);
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, hitLoc.x, hitLoc.y, hitLoc.z, 10, radius/3, radius/3, radius/3, 0.05);
                }
            });

            player.level().addFreshEntity(projectile);
        });
    }

    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(15.0));

        AABB searchBox = player.getBoundingBox().inflate(15.0);
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
