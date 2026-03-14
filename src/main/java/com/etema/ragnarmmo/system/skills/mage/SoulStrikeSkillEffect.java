package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getTarget(player);
        if (target == null || target == player)
            return;

        int hits = (level / 2) + 1; // 1 to 5 hits
        float matkMultiplier = 1.0f + (level * 0.2f);
        float baseDamage = 3.0f * matkMultiplier;

        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            float undeadBonus = 1.0f + (level * 0.05f);
            baseDamage *= undeadBonus;
        }

        final float finalDamage = baseDamage;
        Vec3 start = player.getEyePosition().subtract(0, 0.3, 0);
        
        // Initial Casting Phase (10 ticks of particles)
        for (int t = 0; t < 10; t++) {
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.1, player.getZ(), 8, 0.5, 0.1, 0.5, 0.05);
                    sl.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            });
        }

        for (int i = 0; i < hits; i++) {
            final int hitIndex = i;
            
            // Projectiles start after 10 ticks, then 3 ticks apart
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(10 + i * 3, () -> {
                if (!target.isAlive()) return;
                
                Vec3 currentStart = player.getEyePosition().subtract(0, 0.2, 0);
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
                Vec3 dir = targetPos.subtract(currentStart).normalize();

                com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity projectile = 
                    new com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity(player.level(), player, finalDamage, ParticleTypes.SOUL);
                
                projectile.setSecondaryParticle(ParticleTypes.WITCH);
                projectile.setTarget(target);
                projectile.setHomingStrength(0.12);
                
                projectile.setPos(currentStart.x, currentStart.y, currentStart.z);
                projectile.shoot(dir.x, dir.y, dir.z, 1.4f, 2.0f); // Slight randomness in initial shot for homing to look cool
                
                projectile.setOnHitEffect(result -> {
                    player.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                            net.minecraft.sounds.SoundEvents.SOUL_ESCAPE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f + (hitIndex * 0.1f));

                    if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, result.getLocation().x, result.getLocation().y, result.getLocation().z, 10, 0.2, 0.2, 0.2, 0.05);
                        sl.sendParticles(ParticleTypes.WITCH, result.getLocation().x, result.getLocation().y, result.getLocation().z, 15, 0.3, 0.3, 0.3, 0.1);
                    }
                });

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
