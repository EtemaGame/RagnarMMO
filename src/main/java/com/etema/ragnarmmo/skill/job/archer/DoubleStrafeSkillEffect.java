package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.execution.projectile.ProjectileSkillHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Double Strafe - bow-only two-hit ranged skill.
 */
public class DoubleStrafeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "double_strafe");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0 || !ProjectileSkillHelper.requireBow(player)) {
            return;
        }

        var definition = SkillRegistry.require(ID);
        double damageMultiplier = definition.getLevelDouble("damage_percent", level, 100.0D + (10.0D * level))
                / 100.0D;
        int hitCount = definition.getLevelInt("hit_count", level, 2);
        int hitSpacingTicks = definition.getLevelInt("hit_spacing_ticks", level, 3);

        for (int i = 0; i < hitCount; i++) {
            final int index = i;
            SkillSequencer.schedule(i * hitSpacingTicks, () -> {
                Vec3 look = player.getLookAngle();
                double spreadY = (index == 1) ? 0.005D : 0.0D;
                ProjectileSkillHelper.spawnArrow(player, definition, level,
                        new Vec3(look.x, look.y + spreadY, look.z),
                        2.0D, 3.0F, 0.25F, false, -0.1D, damageMultiplier, 1.0D);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        0.7F, 1.3F + index * 0.2F);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            player.getX() + look.x * 1.5D,
                            player.getY() + 1.2D,
                            player.getZ() + look.z * 1.5D,
                            6, 0.1D, 0.1D, 0.1D, 0.05D);
                }
            });
        }
    }
}
