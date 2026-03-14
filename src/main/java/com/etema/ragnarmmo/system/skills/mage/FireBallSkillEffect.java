package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_ball");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity primaryTarget = getTarget(player);
        if (primaryTarget == null || primaryTarget == player)
            return;

        final double radius = 5.0;
        final float baseDamage = 3.0f + (level * 1.5f);

        // Initial Casting Phase (12 ticks of particles)
        for (int t = 0; t < 12; t++) {
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 5, 0.4, 0.1, 0.4, 0.05);
                    sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.1, player.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(12, () -> {
            if (!primaryTarget.isAlive()) return;

            Vec3 startPos = player.getEyePosition().subtract(0, 0.3, 0);
            Vec3 targetVec = primaryTarget.position().add(0, primaryTarget.getBbHeight() / 2.0, 0);
            Vec3 shootDir = targetVec.subtract(startPos).normalize();

            com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity projectile = 
                new com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity(player.level(), player, baseDamage, ParticleTypes.FLAME);
            
            projectile.setSecondaryParticle(ParticleTypes.SMOKE);
            projectile.setTarget(primaryTarget);
            projectile.setHomingStrength(0.08); // Fireball is heavier, turns slower
            
            projectile.setPos(startPos.x, startPos.y, startPos.z);
            projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.1f, 1.0f); // Fast but with slight spread
            
            projectile.setOnHitEffect(result -> {
                // Impact AOE Damage
                Vec3 hitLoc = result.getLocation();
                AABB area = new AABB(hitLoc.x - radius, hitLoc.y - radius, hitLoc.z - radius,
                                     hitLoc.x + radius, hitLoc.y + radius, hitLoc.z + radius);
                
                List<net.minecraft.world.entity.Entity> nearby = player.level().getEntities(player, area,
                        e -> e instanceof LivingEntity && e != player);

                for (net.minecraft.world.entity.Entity e : nearby) {
                    LivingEntity target = (LivingEntity) e;
                    target.hurt(player.damageSources().indirectMagic(null, player), baseDamage);
                    target.setSecondsOnFire((int) (baseDamage / 2));
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
