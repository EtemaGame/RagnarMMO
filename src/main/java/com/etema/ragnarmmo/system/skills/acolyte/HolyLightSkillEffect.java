package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HolyLightSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "holy_light");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Holy Light is a Quest Skill in RO, max level 1. Deals 125% MATK holy damage.

        LivingEntity target = getTarget(player);
        if (target == null || target == player)
            return; // Must have a target

        final float baseDamage = 6.0f; // Roughly 125% of base magic

        // Initial Casting Phase (8 ticks of particles)
        for (int t = 0; t < 8; t++) {
            com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
                    sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
                }
            });
        }

        com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(8, () -> {
            if (!target.isAlive()) return;

            Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
            Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2.0, 0);
            Vec3 shootDir = targetVec.subtract(startPos).normalize();

            com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity projectile = 
                new com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity(player.level(), player, baseDamage, ParticleTypes.END_ROD);
            
            projectile.setSecondaryParticle(ParticleTypes.INSTANT_EFFECT);
            projectile.setTarget(target);
            projectile.setHomingStrength(0.3); // Holy Light is very precise
            
            projectile.setPos(startPos.x, startPos.y, startPos.z);
            projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 2.5f, 0); 
            
            projectile.setOnHitEffect(result -> {
                player.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLASH, result.getLocation().x, result.getLocation().y, result.getLocation().z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                            40, 0.3, 0.3, 0.3, 0.1);
                }
            });

            player.level().addFreshEntity(projectile);
        });
    }

    // Raycast for target
    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(15.0)); // 15 block range

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
