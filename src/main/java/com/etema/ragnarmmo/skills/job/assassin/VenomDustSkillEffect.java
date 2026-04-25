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

public class VenomDustSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:venom_dust");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Venom Dust: Creates a poison cloud that infects enemies.
        if (player.level() instanceof ServerLevel serverLevel) {
            double range = 2.0 + (level * 0.5);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY(), player.getZ(), 100, range,
                    0.5, range, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                    1.0f, 0.5f);

            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                int baseDuration = 100 + (level * 20);
                int duration = com.etema.ragnarmmo.player.stats.compute.CombatMath.computePoisonDuration(baseDuration, target);
                if (duration > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1));
                }
            }
        }
    }
}
