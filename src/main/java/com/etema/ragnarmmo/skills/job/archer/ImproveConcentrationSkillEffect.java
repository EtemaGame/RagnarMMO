package com.etema.ragnarmmo.skills.job.archer;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.execution.BuffSkillHelper;
import com.etema.ragnarmmo.skills.execution.TargetingSkillHelper;
import com.etema.ragnarmmo.skills.job.mage.SightMobEffect;
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

/**
 * Improve Concentration - DEX/AGI buff plus separate reveal pulse.
 */
public class ImproveConcentrationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_concentration");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        var definition = SkillRegistry.require(ID);
        int durationTicks = definition.getLevelInt("duration_ticks", level, (40 + 20 * level) * 20);
        int effectAmplifier = definition.getLevelInt("effect_amplifier", level, level - 1);
        double revealRadius = definition.getLevelDouble("reveal_radius", level, 7.0D);

        BuffSkillHelper.applyMobEffect(player, RagnarMobEffects.IMPROVE_CONCENTRATION.get(),
                durationTicks, effectAmplifier);

        AABB area = player.getBoundingBox().inflate(revealRadius);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && TargetingSkillHelper.isHostileTo(player, entity));
        for (LivingEntity target : nearby) {
            if (target.getPersistentData().contains(SightMobEffect.CLOAKED_TAG)) {
                target.getPersistentData().remove(SightMobEffect.CLOAKED_TAG);
                target.setInvisible(false);
            }
            target.removeEffect(MobEffects.INVISIBILITY);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, false));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.CONCENTRATION.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    30, 0.6D, 1.0D, 0.6D, 0.08D);
        }
    }
}
