package com.etema.ragnarmmo.skill.job.wizard;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.job.mage.MageTargetUtil;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/**
 * Jupitel Thunder — Active (Wind/Lightning, single target + knockback)
 * RO: Deals 1+level hits of Wind damage and pushes the target back.
 *
 * Fixed from original: now uses proper raycast targeting (not box inflate),
 * each hit is shown with individual particles, knockback is applied once at end.
 */
public class JupitelThunderSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "jupitel_thunder");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = MageTargetUtil.raycast(player, 12.0);
        // Allow shooting even if target is null

        if (!(player.level() instanceof ServerLevel sl)) return;

        int hits = 2 + level; // RO: 3-12 hits depending on level
        float damagePerHit = SkillDamageHelper.scaleByMATK(player, 100.0f);
        float knockbackStrength = 0.2f + (level * 0.05f);

        for (int i = 0; i < hits; i++) {
            int delay = i * 2;
            SkillSequencer.schedule(delay, () -> {
                Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
                Vec3 shootDir;
                
                if (target != null && target.isAlive()) {
                    Vec3 targetVec = target.position().add(0, target.getBbHeight() / 2.0, 0);
                    shootDir = targetVec.subtract(startPos).normalize();
                } else {
                    shootDir = player.getLookAngle();
                }

                AbstractMagicProjectile projectile = 
                    new LightningBoltProjectile(player.level(), player, damagePerHit);
                
                projectile.setProjectileType("jupitel_thunder");
                projectile.setHoming(false);
                projectile.setGravity(0.0f);
                
                projectile.setPos(startPos.x, startPos.y, startPos.z);
                projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 2.0f, 0.5f); // Very fast, accurate
                
                projectile.setOnHitEffect((HitResult result) -> {
                    if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity victim) {
                        victim.knockback(knockbackStrength, player.getX() - victim.getX(), player.getZ() - victim.getZ());
                    }
                    if (player.level() instanceof ServerLevel levelSl) {
                        levelSl.playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
                        levelSl.sendParticles(ParticleTypes.ELECTRIC_SPARK, result.getLocation().x, result.getLocation().y, result.getLocation().z, 10, 0.1, 0.1, 0.1, 0.05);
                    }
                });

                player.level().addFreshEntity(projectile);
            });
        }

        // Initial burst sound
        sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.4f);
    }
}
