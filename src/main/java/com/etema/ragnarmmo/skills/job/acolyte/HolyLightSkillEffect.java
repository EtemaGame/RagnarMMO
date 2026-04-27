package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
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

        var defOpt = SkillRegistry.get(ID);
        LivingEntity target = getTarget(player, defOpt
                .map(def -> def.getLevelDouble("range", level, 15.0D))
                .orElse(15.0D));
        final float damagePercent = defOpt
                .map(def -> (float) def.getLevelDouble("damage_percent", level, 125.0))
                .orElse(125.0f);
        final float baseDamage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, damagePercent);

        // Initial Casting Phase (8 ticks of particles)
        for (int t = 0; t < 8; t++) {
            final int tick = t;
            com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(t, () -> {
                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
                    sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0, 0, 0, 0);
                    SkillVisualFx.spawnRotatingRing(sl, player.position(), 0.85, 0.15, ParticleTypes.GLOW, 8, tick * 0.35);
                }
            });
        }

        com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(8, () -> {
            Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
            Vec3 shootDir;
            
            if (target != null && target.isAlive()) {
                Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2.0, 0);
                shootDir = targetVec.subtract(startPos).normalize();
            } else {
                shootDir = player.getLookAngle();
            }

            com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity projectile =
                new com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity(player.level(), player, baseDamage, ParticleTypes.END_ROD);
            
            projectile.setSkillId(ID);
            projectile.setSecondaryParticle(ParticleTypes.INSTANT_EFFECT);
            projectile.setProjectileType("holy_light");
            projectile.setHoming(false);
            projectile.setGravity(0.0f);
            
            projectile.setPos(startPos.x, startPos.y, startPos.z);
            projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 2.5f, 0.0f); // Very fast, pinpoint accurate
            
            projectile.setOnHitEffect(result -> {
                Vec3 impact = result != null ? result.getLocation() : projectile.position();
                player.level().playSound(null, impact.x, impact.y, impact.z,
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y, impact.z,
                            40, 0.3, 0.3, 0.3, 0.1);
                    SkillVisualFx.spawnVerticalCross(sl, impact, 0.0, 1.5, 0.35, ParticleTypes.END_ROD, ParticleTypes.GLOW);
                }
            });

            player.level().addFreshEntity(projectile);
        });
    }

    // Raycast for target
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
