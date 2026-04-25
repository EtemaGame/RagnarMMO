package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Spear Stab — Active
 * RO: Powerful linear thrust that pushes enemies back (and may push them into the wall for bonus damage).
 *
 * Minecraft:
 *  - Single target hit in front of the player (4.5 block range).
 *  - High damage + strong knockback away from player.
 *  - If target collides with a wall within 1s, bonus damage occurs (approximated by checking if
 *    velocity was zeroed out by the collision — we keep it simple: apply base damage + flat bonus).
 */
public class SpearStabSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "spear_stab");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = PierceSkillEffect.getClosestTarget(player, 4.5);
        if (target == null) return;

        // RO: (200 + 50×level)% ATK — a powerful spear thrust
        float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, 200f + 50f * level));
        com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                target, player.damageSources().playerAttack(player), damage);

        // Strong knockback in the look direction
        Vec3 dir = player.getLookAngle().normalize();
        target.knockback(2.5f, -dir.x, -dir.z);

        // Sounds
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.0f);

        // Particles: spear thrust effect
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 midPoint = player.position().add(dir.scale(2.0)).add(0, 1.0, 0);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    midPoint.x, midPoint.y, midPoint.z,
                    3, 0.1, 0.1, 0.1, 0.0);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
        }
    }
}
