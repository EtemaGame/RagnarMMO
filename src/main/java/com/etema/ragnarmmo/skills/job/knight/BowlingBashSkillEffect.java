package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "bowling_bash");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity primary = PierceSkillEffect.getClosestTarget(player, 4.0);
        if (primary == null) return;

        // Ensure serverLevel is available for particle and entity checks
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // RO: (500 + 50×level)% ATK — Bowling Bash is a massive burst AoE
        float pct = 500f + (50f * level);
        float primaryDamage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, pct));
        
        com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                primary, player.damageSources().playerAttack(player), primaryDamage);

        // Strong knockback toward the intersection direction
        Vec3 knockDir = primary.position().subtract(player.position()).normalize();
        primary.knockback(1.5f, -knockDir.x, -knockDir.z);

        // --- Gut Hit / Bowling Chain Logic ---
        // RO: If a mob is near the primary, they "collide" for a second hit.
        // We detect this by inflating the primary's box slightly.
        AABB collisionBox = primary.getBoundingBox().inflate(1.2);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, collisionBox,
                e -> e != player && e.isAlive());

        boolean hasChain = targets.size() > 1; // Primary + at least one more
        List<LivingEntity> chainHit = new ArrayList<>();

        if (hasChain) {
            float chainDamage = primaryDamage; // In RO, BB deals 2 hits of the same damage on collision
            for (LivingEntity target : targets) {
                if (target == primary) {
                    // Second hit on primary
                    com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                            target, player.damageSources().playerAttack(player), chainDamage);
                } else {
                    // Hit on the bowled mobs
                    com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                            target, player.damageSources().playerAttack(player), chainDamage);
                    target.knockback(1.0f, -knockDir.x, -knockDir.z);
                    chainHit.add(target);
                }
                if (chainHit.size() >= 5) break; // Cap chain to 5 additional mobs
            }
        }

        // --- 3. PARTICLES & FEEDBACK ---
        // RO-style "Wave" air blast
        for (int j = 0; j < 5; j++) {
            double dist = 1.0 + j * 0.8;
            double spread = 0.5 + j * 0.2;
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX() + knockDir.x * dist, player.getY() + 0.8, player.getZ() + knockDir.z * dist,
                    3, spread, 0.1, spread, 0.01);
        }

        // Impact burst at primary target
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                primary.getX(), primary.getY() + 1, primary.getZ(),
                2, 0.3, 0.3, 0.3, 0.0);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                primary.getX(), primary.getY() + 1, primary.getZ(),
                15, 0.5, 0.4, 0.5, 0.2);
        
        // Chain impacts
        for (LivingEntity c : chainHit) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    c.getX(), c.getY() + 1, c.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        }

        // Sounds
        player.level().playSound(null, primary.getX(), primary.getY(), primary.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.7f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
