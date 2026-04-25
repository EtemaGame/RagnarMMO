package com.etema.ragnarmmo.skills.job.archer;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.execution.TargetingSkillHelper;
import com.etema.ragnarmmo.skills.execution.projectile.ProjectileSkillHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arrow Shower - adapted area cleanup/control skill.
 */
public class ArrowShowerSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "arrow_shower");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0 || !ProjectileSkillHelper.requireBow(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var definition = SkillRegistry.require(ID);
        double range = definition.getLevelDouble("range", level, 12.0D);
        Vec3 center = TargetingSkillHelper.targetPoint(player, range);
        double radius = definition.getLevelDouble("aoe_radius", level, 3.0D);
        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 75.0D + (5.0D * level));
        double knockbackStrength = definition.getLevelDouble("knockback_strength", level, 0.35D);
        float damage = Math.max(SkillDamageHelper.MIN_ATK, SkillDamageHelper.scaleByATK(player, damagePercent));

        List<LivingEntity> targets = TargetingSkillHelper.livingAround(player, center, radius, 2.0D,
                entity -> TargetingSkillHelper.isHostileTo(player, entity));

        for (LivingEntity victim : targets) {
            SkillDamageHelper.dealSkillDamage(victim, player.damageSources().playerAttack(player), damage);

            if (knockbackStrength > 0.0D) {
                Vec3 knockDir = victim.position().subtract(center).normalize();
                victim.knockback(knockbackStrength, -knockDir.x, -knockDir.z);
            }

            serverLevel.sendParticles(ParticleTypes.CRIT,
                    victim.getX(), victim.getY() + 1.0D, victim.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.1D);
        }

        serverLevel.playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.6F);

        for (int i = 0; i < 20; i++) {
            double rx = center.x + (player.getRandom().nextDouble() - 0.5D) * radius * 1.8D;
            double rz = center.z + (player.getRandom().nextDouble() - 0.5D) * radius * 1.8D;
            serverLevel.sendParticles(ParticleTypes.CRIT, rx, center.y + 4.0D, rz, 1, 0.0D, -1.0D, 0.0D, 0.6D);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, rx, center.y + 0.5D, rz,
                    2, 0.1D, 0.1D, 0.1D, 0.0D);
        }
    }
}
