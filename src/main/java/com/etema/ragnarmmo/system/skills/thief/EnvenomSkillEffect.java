package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EnvenomSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "envenom");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getMeleeTarget(player);
        if (target == null)
            return;

        // Envenom: Deals Poison elemental damage + bonus flat damage and inflicts
        // Poison state.
        // Damage multiplier: 115% to 250% + 15 Flat.

        float damageMultiplier = 1.0f + (level * 0.15f);
        float baseDamage = (4.0f * damageMultiplier) + 2.0f; // Approx flat addition

        // Introduce a slight delay for the "slash" feel
        com.etema.ragnarmmo.system.skills.SkillSequencer.schedule(2, () -> {
            if (!target.isAlive()) return;

            target.hurt(player.damageSources().mobAttack(player), baseDamage);

            // 14% to 50% chance to poison
            float poisonChance = 0.10f + (level * 0.04f);
            if (player.getRandom().nextFloat() < poisonChance) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 200 + (level * 20), 0, false, true, true));
            }

            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Purple toxic burst
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, target.getX(), target.getY() + 1.0, target.getZ(), 
                        30, 0.3, 0.5, 0.3, 0.1);
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1.2, target.getZ(), 
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        });
    }

    // (Helper duplicated for clarity across active melee skills)
    private LivingEntity getMeleeTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(3.5));

        AABB searchBox = player.getBoundingBox().inflate(3.5);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
