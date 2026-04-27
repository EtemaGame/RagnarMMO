package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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

import java.util.List;

public class VenomKnifeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:venom_knife");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Venom Knife: Throws a poisoned knife.
        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2,
                    0.2, 0.2, 0.1);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);

            target.hurt(player.damageSources().playerAttack(player), 5.0f + level);
            int duration = com.etema.ragnarmmo.player.stats.compute.CombatMath.computePoisonDuration(100, target);
            if (duration > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1));
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
