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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sonic_blow");
    private static final int HIT_COUNT = 8;

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
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
