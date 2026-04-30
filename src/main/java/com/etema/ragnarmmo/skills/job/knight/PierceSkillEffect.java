package com.etema.ragnarmmo.skills.job.knight;

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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "pierce");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
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
