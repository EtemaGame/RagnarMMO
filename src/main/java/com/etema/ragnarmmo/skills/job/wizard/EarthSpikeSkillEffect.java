package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EarthSpikeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "earth_spike");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        // Earth Spike: Rock spikes up from the ground.
        // RO: Deals Matk * 1 per Skill Level to a single target.
        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        float damagePerHit = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 100.0f);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < level; i++) {
                int delay = i * 3;
                com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(delay, () -> {
                    if (target == null || !target.isAlive()) return;
                    
                    com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                        new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, damagePerHit, 
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()));
                    
                    projectile.setProjectileType("earth_spike");
                    projectile.setHoming(false);
                    projectile.setGravity(0.0f);
                    
                    projectile.setPos(target.getX(), target.getY() - 1.0, target.getZ());
                    projectile.shoot(0, 1, 0, 0.5f, 0.0f); // Slow rising spike
                    
                    projectile.setOnHitEffect(result -> {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                                result.getLocation().x, result.getLocation().y, result.getLocation().z, 40, 0.5, 0.5, 0.5, 0.1);
                        serverLevel.playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, 
                                SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
                        
                        if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity victim) {
                            victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.3, 0));
                        }
                    });

                    player.level().addFreshEntity(projectile);
                });
            }
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
