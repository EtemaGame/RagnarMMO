package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;

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
 * Pierce - Active spear skill (Knight).
 *
 * <p>Uses the shared RO size resolver so small, medium, and large targets are
 * treated consistently across normal attacks, skill info, and damage formulas.</p>
 */
public class PierceSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "pierce");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        LivingEntity target = getClosestTarget(player, 4.5);
        if (target == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int hits = switch (CombatPropertyResolver.getEntitySize(target)) {
            case SMALL -> 1;
            case MEDIUM -> 2;
            case LARGE -> 3;
        };

        float pctPerHit = 100.0f + (10.0f * level);
        float damagePerHit = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, pctPerHit));

        for (int i = 0; i < hits; i++) {
            final int hitIndex = i;
            SkillSequencer.schedule(i * 3, () -> {
                if (!target.isAlive()) {
                    return;
                }

                SkillDamageHelper.dealSkillDamage(target,
                        player.damageSources().playerAttack(player), damagePerHit);

                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS,
                        0.9f, 1.1f + hitIndex * 0.15f);

                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.08);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        4, 0.1, 0.15, 0.1, 0.05);

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
                entity -> entity != player && entity.isAlive());

        return candidates.stream()
                .filter(entity -> entity.getBoundingBox().inflate(entity.getPickRadius() + 0.5).clip(start, end).isPresent()
                        || entity.getBoundingBox().contains(start))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }
}
