package com.etema.ragnarmmo.system.skills.priest;

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

public class ImpositioManusSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:impositio_manus");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Impositio Manus: Increases ATK.
        LivingEntity target = getClosestTarget(player, 5.0);
        if (target == null)
            target = player;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.INSTANT_EFFECT, target.getX(), target.getY() + 1.0, target.getZ(),
                    30, 0.3, 0.3, 0.3, 0.1);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0f,
                    2.0f);

            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, level / 2));
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
