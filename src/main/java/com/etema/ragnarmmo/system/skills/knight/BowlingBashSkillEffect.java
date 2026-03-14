package com.etema.ragnarmmo.system.skills.knight;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bowling Bash — Active (Chain AoE knockback)
 * RO: High damage + knockback. Enemies hit by the primary target can trigger
 *     secondary hits as the target "bowls" into adjacent enemies.
 *     Damage = 400% + 100%×level ATK.
 *
 * Minecraft:
 *  - Primary: Heavy hit on closest target in front (4 block range), sending them flying.
 *  - Secondary: If the primary target's knockback trajectory hits another mob within 3 blocks,
 *    that mob also takes damage (chain effect).
 *  - Particle cone radiating forward to illustrate the "bowling" motion.
 */
public class BowlingBashSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "bowling_bash");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity primary = PierceSkillEffect.getClosestTarget(player, 4.0);
        if (primary == null) return;

        // Primary damage: high scaling
        float primaryDamage = 5.0f + (level * 1.5f);
        primary.hurt(player.damageSources().playerAttack(player), primaryDamage);

        // Strong knockback toward the enemy's current direction (bowling effect)
        Vec3 knockDir = primary.position().subtract(player.position()).normalize();
        primary.knockback(3.0f, -knockDir.x, -knockDir.z);

        // --- Chain (secondary bounce): targets near the knockback trajectory ---
        Vec3 bounceTarget = primary.position().add(knockDir.scale(3.0));
        AABB chainBox = new AABB(primary.position(), bounceTarget).inflate(1.5);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, chainBox,
                e -> e != player && e != primary && e.isAlive());

        float chainDamage = primaryDamage * 0.5f; // 50% of primary on chain hit
        List<LivingEntity> chainHit = new ArrayList<>();
        for (LivingEntity chainTarget : nearby) {
            chainTarget.hurt(player.damageSources().playerAttack(player), chainDamage);
            chainTarget.knockback(1.0f, -knockDir.x, -knockDir.z);
            chainHit.add(chainTarget);
            // Only chain hits up to 3 enemies
            if (chainHit.size() >= 3) break;
        }

        // --- Particles ---
        if (player.level() instanceof ServerLevel serverLevel) {
            // Cone going forward from player
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX() + knockDir.x * 2, player.getY() + 1, player.getZ() + knockDir.z * 2,
                    4, 0.2, 0.2, 0.2, 0.0);
            // Impact burst at primary target
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    primary.getX(), primary.getY() + 1, primary.getZ(),
                    2, 0.3, 0.3, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    primary.getX(), primary.getY() + 1, primary.getZ(),
                    15, 0.5, 0.5, 0.5, 0.15);
            // Chain impacts
            for (LivingEntity c : chainHit) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        c.getX(), c.getY() + 1, c.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // Sounds
        player.level().playSound(null, primary.getX(), primary.getY(), primary.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.7f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
