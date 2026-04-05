package com.etema.ragnarmmo.skill.job.assassin;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class VenomSplasherSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:venom_splasher");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Venom Splasher: Explodes a poisoned target after a delay.
        // For simplicity, we'll deal immediate high damage to a target if it has
        // poison.
        LivingEntity target = getClosestTarget(player, 5.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 5,
                        0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                        1.0f, 1.5f);
                target.hurt(player.damageSources().playerAttack(player), 20.0f + (level * 10.0f));
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
