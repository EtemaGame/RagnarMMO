package com.etema.ragnarmmo.skill.job.priest;

import java.util.List;

import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class AspersionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "aspersion");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        LivingEntity target = getClosestTarget(player, 5.0);
        if (target == null) {
            target = player;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            int durationTicks = 600 + (level * 200);
            long untilTick = player.level().getGameTime() + durationTicks;

            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(),
                    40, 0.5, 0.5, 0.5, 0.1);
            serverLevel.playSound(null, target.blockPosition(), RagnarSounds.ASPERSION.get(), SoundSource.PLAYERS,
                    1.0f, 1.0f);

            target.addEffect(new MobEffectInstance(RagnarMobEffects.ASPERSION.get(), durationTicks, level - 1));
            CombatPropertyResolver.applyTemporaryWeaponElement(target, ElementType.HOLY, untilTick);
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(0);
    }
}
