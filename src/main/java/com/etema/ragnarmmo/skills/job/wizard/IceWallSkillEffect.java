package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Ice Wall — Active
 */
public class IceWallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "ice_wall");

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
            new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, 0, ParticleTypes.SNOWFLAKE);
        
        projectile.setProjectileType("icewall");
        projectile.setHoming(false);
        projectile.setGravity(0.01f);
        
        projectile.setPos(startPos.x, startPos.y, startPos.z);
        projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.0f, 0.0f);
        
        projectile.setOnHitEffect(result -> {
            BlockPos base = BlockPos.containing(result.getLocation());
            for (int i = -1; i <= 1; i++) {
                BlockPos p = base.relative(player.getDirection().getClockWise(), i);
                if (player.level().getBlockState(p).isAir()) {
                    player.level().setBlock(p, Blocks.ICE.defaultBlockState(), 3);
                }
                if (player.level().getBlockState(p.above()).isAir()) {
                    player.level().setBlock(p.above(), Blocks.ICE.defaultBlockState(), 3);
                }
            }
        });

        player.level().addFreshEntity(projectile);
    }
}
