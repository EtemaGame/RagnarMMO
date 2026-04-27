package com.etema.ragnarmmo.skills.job.priest;

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

public class LexDivinaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:lex_divina");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Lex Divina: Silences the target (prevents casting).
        // For Minecraft, we'll apply Weakness and Blindness.
        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.5,
                    1.0, 0.5, 0.05);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                    1.0f, 1.0f);

            int durationTicks = (10 + level * 2) * 20;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0));
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
