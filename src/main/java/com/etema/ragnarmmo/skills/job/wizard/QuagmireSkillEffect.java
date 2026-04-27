package com.etema.ragnarmmo.skills.job.wizard;

import net.minecraft.core.BlockPos;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Quagmire — Active
 */
public class QuagmireSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "quagmire");

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
            new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, 0, ParticleTypes.SQUID_INK);
        
        projectile.setProjectileType("quagmire");
        projectile.setHoming(false);
        projectile.setGravity(0.04f); // Ballistic arc
        
        projectile.setPos(startPos.x, startPos.y, startPos.z);
        projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 0.8f, 0.0f);
        
        projectile.setOnHitEffect(result -> {
            Vec3 hitLoc = result.getLocation();
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, hitLoc.x, hitLoc.y, hitLoc.z, 50, 4.0, 0.1, 4.0, 0.05);
                serverLevel.playSound(null, BlockPos.containing(hitLoc), SoundEvents.MUD_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);

                AABB area = new AABB(hitLoc.x - 4.0, hitLoc.y - 1.0, hitLoc.z - 4.0, hitLoc.x + 4.0, hitLoc.y + 2.0, hitLoc.z + 4.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2 + level / 2));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, level / 3));
                }
            }
        });

        player.level().addFreshEntity(projectile);
    }
}
