package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SandAttackSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sand_attack");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Sand Attack: Deals 130% ATK damage (Earth element) and has a 15% chance to
        // Blind.

        LivingEntity target = getMeleeTarget(player);
        if (target == null)
            return;

        float baseDamage = 4.0f * 1.30f; // 130% proxy

        target.hurt(player.damageSources().mobAttack(player), baseDamage);

        if (player.getRandom().nextFloat() <= 0.15f) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, true, true)); // 10 seconds
                                                                                                      // blind
        }

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    target.getX(), target.getY() + 1.5, target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
        }
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
