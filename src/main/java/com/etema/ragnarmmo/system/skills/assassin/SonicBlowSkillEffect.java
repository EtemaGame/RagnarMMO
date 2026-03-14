package com.etema.ragnarmmo.system.skills.assassin;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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

public class SonicBlowSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:sonic_blow");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Sonic Blow: 8 rapidly hitting strikes. Stuns the target.
        LivingEntity target = getClosestTarget(player, 4.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            // Visual feedback: rapid particles and sounds
            for (int i = 0; i < 8; i++) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(),
                        1, 0.2, 0.2, 0.2, 0.1);
            }
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                    1.0f, 1.5f);

            // Damage calculation: level-based multiplier
            float damageMult = 3.0f + (level * 0.5f);
            target.hurt(player.damageSources().playerAttack(player), 10.0f * damageMult);

            // Stun effect (Slowness + Weakness in MC)
            if (serverLevel.random.nextFloat() < 0.12 + (level * 0.02)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 4));
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
