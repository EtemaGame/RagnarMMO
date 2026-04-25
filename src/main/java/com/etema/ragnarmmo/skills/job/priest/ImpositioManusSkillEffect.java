package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ImpositioManusSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "impositio_manus");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Impositio Manus: Increases ATK.
        LivingEntity target = getClosestTarget(player, 5.0);
        if (target == null)
            target = player;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT, target.getX(), target.getY() + 1.0, target.getZ(),
                    30, 0.3, 0.3, 0.3, 0.1);
            serverLevel.playSound(null, target.blockPosition(), RagnarSounds.IMPOSITIO_MANUS.get(), SoundSource.PLAYERS, 1.0f,
                    1.0f);

            target.addEffect(new MobEffectInstance(RagnarMobEffects.IMPOSITIO_MANUS.get(), 1200, level - 1));
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
