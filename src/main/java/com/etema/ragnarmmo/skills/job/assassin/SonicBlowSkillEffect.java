package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Sonic Blow — Active multi-hit melee (Assassin / Assassin Cross).
 *
 * <p><b>RO Formula:</b> 8 rapid hits, each dealing (150 + 40 × level) / 8 % ATK.
 * Total damage = (150 + 40×L)% ATK.
 * Stun chance on final hit = 12 + 2×level %.
 *
 * <p><b>Multi-hit fix:</b> Uses {@link SkillDamageHelper#dealSkillDamage} to bypass
 * Minecraft's 10-tick invincibility frames ({@code hurtTime}) between each hit.
 * Hits are still scheduled 2 ticks apart for visual/audio feedback.
 */
public class SonicBlowSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "sonic_blow");
    private static final int HIT_COUNT = 8;

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        LivingEntity target = getClosestTarget(player, 2.5);
        if (target == null) return;

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // RO: total damage = (300 + 50 × level)% ATK, split evenly across 8 hits.
        float totalPct = 300.0f + (50.0f * level);       // e.g. Lv1 = 350%, Lv10 = 800%
        float pctPerHit = totalPct / HIT_COUNT;
        float damagePerHit = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, pctPerHit));

        float stunChance = 0.12f + (0.02f * level);

        for (int i = 0; i < HIT_COUNT; i++) {
            final int hitIndex = i;
            // 2-tick spacing for visual blur (each hit MUST use dealSkillDamage to bypass hurtTime)
            SkillSequencer.schedule(i * 2, () -> {
                if (!target.isAlive()) return;

                // Bypass hurtTime so all 8 hits register
                SkillDamageHelper.dealSkillDamage(target,
                        player.damageSources().playerAttack(player), damagePerHit);

                // Visual and audio feedback
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                        0.8f, 1.1f + hitIndex * 0.08f);

                double ox = (player.getRandom().nextDouble() - 0.5) * 0.5;
                double oy = (player.getRandom().nextDouble()) * 0.6;
                double oz = (player.getRandom().nextDouble() - 0.5) * 0.5;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        2, 0.15, 0.2, 0.15, 0.08);

                // Final hit: stun chance + big impact effect
                if (hitIndex == HIT_COUNT - 1) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            target.getX(), target.getY() + 1.2, target.getZ(),
                            15, 0.3, 0.3, 0.3, 0.12);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.4f);

                    float finalStunChance = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeStunChance(stunChance, target);
                    int finalStunDuration = com.etema.ragnarmmo.player.stats.compute.CombatMath.computeStunDuration(60, target);

                    if (finalStunDuration > 0 && player.level().random.nextFloat() < finalStunChance) {
                        long until = player.level().getGameTime() + finalStunDuration;
                        target.getPersistentData().putLong("ragnarmmo_stunned_until", until);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, finalStunDuration, 10, false, false));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(1, finalStunDuration - 20), 1, false, true, false));
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.8f);
                    }
                }
            });
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(e.getPickRadius() + 0.5).clip(start, end).isPresent()
                        || e.getBoundingBox().contains(start))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(candidates.isEmpty() ? null : candidates.stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                        .orElse(null));
    }
}
