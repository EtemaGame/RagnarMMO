package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
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

        var defOpt = SkillRegistry.get(ID);
        LivingEntity primaryTarget = getTarget(player);
        final double radius = defOpt
                .map(def -> def.getLevelDouble("splash_radius", level, 2.5))
                .orElse(2.5);
        final int burnSeconds = defOpt
                .map(def -> def.getLevelInt("burn_seconds", level, 3))
                .orElse(3);

        // Particles for launch
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.1, player.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.1, player.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            SkillVisualFx.spawnRotatingRing(sl, player.position(), 1.15, 0.1, ParticleTypes.SMALL_FLAME, 10, 0.0);
            SkillVisualFx.spawnRotatingRing(sl, player.position(), 0.7, 1.0, ParticleTypes.LAVA, 6, Math.PI / 8.0);
        }

        Vec3 startPos = player.getEyePosition().subtract(0, 0.3, 0);
        Vec3 shootDir;
        
        if (primaryTarget != null && primaryTarget.isAlive()) {
            Vec3 targetVec = primaryTarget.position().add(0, primaryTarget.getBbHeight() / 2.0, 0);
            shootDir = targetVec.subtract(startPos).normalize();
        } else {
            shootDir = player.getLookAngle();
        }

        com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity projectile =
            new com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity(player.level(), player, 0.0f, ParticleTypes.FLAME);

        projectile.setSkillId(ID);
        projectile.setSecondaryParticle(ParticleTypes.SMOKE);
        projectile.setProjectileType("fireball");
        projectile.setHoming(false);
        projectile.setGravity(0.0f);
        
        projectile.setPos(startPos.x, startPos.y, startPos.z);
        projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.3f, 0.0f);
        
        projectile.setOnHitEffect(result -> {
            Vec3 hitLoc = result != null ? result.getLocation() : projectile.position();
            AABB area = new AABB(hitLoc.x - radius, hitLoc.y - radius, hitLoc.z - radius,
                                 hitLoc.x + radius, hitLoc.y + radius, hitLoc.z + radius);
            
            List<net.minecraft.world.entity.Entity> nearby = player.level().getEntities(player, area,
                    e -> e instanceof LivingEntity && e != player);

            for (net.minecraft.world.entity.Entity e : nearby) {
                LivingEntity target = (LivingEntity) e;
                target.setSecondsOnFire(burnSeconds);
            }

            player.level().playSound(null, hitLoc.x, hitLoc.y, hitLoc.z,
                    com.etema.ragnarmmo.common.init.RagnarSounds.FIRE_BALL.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, hitLoc.x, hitLoc.y, hitLoc.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME, hitLoc.x, hitLoc.y, hitLoc.z, 25, 0.5, 0.5, 0.5, 0.1);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
                        hitLoc.x, hitLoc.y, hitLoc.z, 18, 0.45, 0.35, 0.45, 0.08);
                SkillVisualFx.spawnRotatingRing(serverLevel, hitLoc, radius * 0.55, 0.15, ParticleTypes.FLAME, 12, 0.0);
            }
        });

        player.level().addFreshEntity(projectile);
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
