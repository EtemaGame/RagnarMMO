package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ShatteringStrikeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "shattering_strike");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        LivingEntity target = getClosestTarget(player, 4.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.2,
                    0.2, 0.2, 0.2);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 1.0f,
                    1.0f);

            target.hurt(player.damageSources().playerAttack(player), 15.0f + (level * 5.0f));

            // Chance to "break" (apply weakness/slowness)
            if (serverLevel.random.nextFloat() < 0.05 + (level * 0.05)) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 2));
            }
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
