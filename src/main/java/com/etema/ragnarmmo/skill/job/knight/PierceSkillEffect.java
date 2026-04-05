package com.etema.ragnarmmo.skill.job.knight;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Pierce — Active spear skill (Knight).
 *
 * <p><b>RO Formula:</b> Hits same target N times based on body size:
 * <ul>
 *   <li>Small (volume ≤ 0.8): 1 hit</li>
 *   <li>Medium (volume ≤ 3.0): 2 hits</li>
 *   <li>Large (volume > 3.0): 3 hits</li>
 * </ul>
 * Each hit deals {@code (100 + 10 × level)} % ATK.
 *
 * <p><b>Multi-hit fix:</b> Uses {@link SkillDamageHelper#dealSkillDamage} to bypass
 * hurtTime on every hit, and schedules hits via {@link SkillSequencer} (3 ticks apart)
 * instead of a synchronous loop. Both changes are necessary — the old synchronous loop
 * landed all N hits in the same tick while the target still had 10 ticks of invincibility
 * from the first hit.
 */
public class PierceSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "pierce");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = getClosestTarget(player, 4.5);
        if (target == null) return;

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Determine hit count by target size (bounding box volume approximation)
        double volume = target.getBbHeight() * target.getBbWidth() * target.getBbWidth();
        int hits;
        if (volume > 3.0)      hits = 3; // Large mobs: Creeper, Witch, most tall mobs
        else if (volume > 0.8) hits = 2; // Medium: Skeleton, Zombie, Player
        else                   hits = 1; // Small: Bat, Silverfish, baby mobs

        // RO: (100 + 10 × level)% ATK per hit
        float pctPerHit = 100.0f + (10.0f * level);
        float damagePerHit = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, pctPerHit));

        for (int i = 0; i < hits; i++) {
            final int hitIndex = i;
            // Schedule 3 ticks apart — combined with dealSkillDamage this ensures
            // every hit both (a) looks staggered and (b) actually registers damage.
            SkillSequencer.schedule(i * 3, () -> {
                if (!target.isAlive()) return;

                // Bypass hurtTime so each stab registers
                SkillDamageHelper.dealSkillDamage(target,
                        player.damageSources().playerAttack(player), damagePerHit);

                // Stab impact sound and particles
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS,
                        0.9f, 1.1f + hitIndex * 0.15f);

                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.08);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        4, 0.1, 0.15, 0.1, 0.05);

                // Final hit extra impact
                if (hitIndex == hits - 1 && hits > 1) {
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.2f);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + 1.3, target.getZ(),
                            1, 0, 0, 0, 0);
                }
            });
        }
    }

    static LivingEntity getClosestTarget(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(e.getPickRadius() + 0.5).clip(start, end).isPresent()
                        || e.getBoundingBox().contains(start))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}
