package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.execution.projectile.ProjectileSkillHelper;
import com.etema.ragnarmmo.skill.execution.projectile.RagnarArrowSpawnHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import com.etema.ragnarmmo.roitems.runtime.RangedWeaponStatsHelper;
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
        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 100.0D + (10.0D * level));
        float scaledDamage = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, damagePercent));
        int hitCount = definition.getLevelInt("hit_count", level, 2);
        int hitSpacingTicks = definition.getLevelInt("hit_spacing_ticks", level, 3);

        for (int i = 0; i < hitCount; i++) {
            final int index = i;
            SkillSequencer.schedule(i * hitSpacingTicks, () -> {
                Vec3 look = player.getLookAngle();
                double spreadY = (index == 1) ? 0.005D : 0.0D;
                RagnarArrowSpawnHelper.spawn(player, definition, level,
                        new Vec3(look.x, look.y + spreadY, look.z),
                        3.0F, 0.25F, 1.0F, arrow -> {
                            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
                        }, snapshot -> {
                            snapshot.putBoolean("bypass_iframes", true);
                            snapshot.putString("damage_mode", RangedWeaponStatsHelper.DAMAGE_MODE_ATK_OVERRIDE);
                            snapshot.putDouble("atk_override", scaledDamage);
                        });

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
