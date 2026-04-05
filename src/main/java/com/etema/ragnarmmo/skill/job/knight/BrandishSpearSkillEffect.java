package com.etema.ragnarmmo.skill.job.knight;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Brandish Spear — Active (Wide AoE swing)
 * RO: Swings the spear in a 3×3 area pattern up to 7×7 at high levels.
 *     Best used while mounted (Peco Peco) for a bonus radius.
 *     Damage = (Spear ATK) × (100 + 40×level)%
 *
 * Minecraft:
 *  - AoE around the player in a frontal cone (5 blocks base radius).
 *  - Deals damage to all entities in range.
 *  - If the player is riding a mount, radius is doubled (Peco Peco equivalent).
 *  - Sweeping arc particles radiating from the player center.
 */
public class BrandishSpearSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "brandish_spear");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Base radius: grows with level. Mounted = extra range.
        double radius = 3.0 + (level * 0.3);
        boolean isMounted = player.isPassenger(); // Riding any vehicle = mounted equivalent
        if (isMounted) radius *= 1.5;

        // RO: (100 + 40×level)% ATK per target in the AOE sweep
        float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, 100f + 40f * level));

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                    target, player.damageSources().playerAttack(player), damage);

            // Minor knockback outward (spear swing pushes away)
            Vec3 knockDir = target.position().subtract(player.position()).normalize();
            target.knockback(0.6f, -knockDir.x, -knockDir.z);
        }

        // --- Particles: circular sweep arc around the player ---
        if (player.level() instanceof ServerLevel serverLevel) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double px = player.getX() + Math.cos(angle) * radius;
                double pz = player.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        px, player.getY() + 1.2, pz, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        px, player.getY() + 1.0, pz, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Sounds
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6f, 1.3f);
    }
}
