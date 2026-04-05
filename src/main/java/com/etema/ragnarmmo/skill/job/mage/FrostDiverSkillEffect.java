package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Frost Diver — Active (Water property)
 */
public class FrostDiverSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "frost_diver");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
        Vec3 shootDir = player.getLookAngle();

        com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
            new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, 4.0f + (level * 1.5f), ParticleTypes.SNOWFLAKE);
        
        projectile.setProjectileType("icebolt"); // Reuse icebolt texture
        projectile.setHoming(false);
        projectile.setGravity(0.0f);
        
        projectile.setPos(startPos.x, startPos.y, startPos.z);
        projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.5f, 0.0f);
        
        projectile.setOnHitEffect(result -> {
            if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                float freezeChance = 0.38f + (level * 0.03f);
                if (player.getRandom().nextFloat() <= freezeChance) {
                    int durationTicks = (3 + (int) (level * 0.5)) * 20;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 4, false, true, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1, false, true, false));
                    target.setTicksFrozen(durationTicks);
                }
            }
            player.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
        });

        player.level().addFreshEntity(projectile);
    }
}
