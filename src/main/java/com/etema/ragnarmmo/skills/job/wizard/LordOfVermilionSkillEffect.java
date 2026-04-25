package com.etema.ragnarmmo.skills.job.wizard;

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

/**
 * Lord of Vermilion — Active (Wind, massive storm)
 * RO: Massive AoE wind+thunder storm, hits multiple times, 10% blind chance per hit.
 *
 * Fixed from original: missing Blind proc and using better sound sequencing.
 */
public class LordOfVermilionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "lord_of_vermillion");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        AABB area = player.getBoundingBox().inflate(10.0);
        if (!(player.level() instanceof ServerLevel sl)) return;

        // Particles: massive electric storm
        sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 3, player.getZ(),
                10, 5.0, 1.0, 5.0, 0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(),
                400, 8.0, 2.0, 8.0, 0.15);

        // Sounds: thunder + impact sequence
        sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.8f);
        sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f, 1.0f);

        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        float damagePerHit = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, (80.0f + 20.0f * level) / 10.0f);

        for (int wave = 0; wave < 4; wave++) {
            int waveDelay = wave * 20; // 1 second between waves
            com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(waveDelay, () -> {
                // Wave particles
                sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 3, player.getZ(), 5, 5.0, 1.0, 5.0, 0);
                sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);

                for (int hit = 0; hit < 10; hit++) {
                    int hitDelay = hit * 1;
                    com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(hitDelay, () -> {
                        List<LivingEntity> waveTargets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
                        for (LivingEntity target : waveTargets) {
                            // Use dealSkillDamage to bypass hurtTime for the 40 hits
                            com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, player.damageSources().indirectMagic(null, player), damagePerHit);

                            // 10% blind chance per hit (RO mechanic)
                            if (player.getRandom().nextFloat() < 0.10f) {
                                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60 + level * 10, 0, false, true, true));
                            }

                            // Individual lightning strike visual
                            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 5, 0.1, 0.2, 0.1, 0.05);
                        }
                    });
                }
            });
        }
    }
}
