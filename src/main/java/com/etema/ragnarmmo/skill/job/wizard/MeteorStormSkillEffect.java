package com.etema.ragnarmmo.skill.job.wizard;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Meteor Storm — Active (Multi-hit Fire Rain)
 * RO: Drops 2×Level meteors in a random area around the caster.
 *     Each hit deals MATK×(100+40×Level)% and has a 30% chance to burn.
 *
 * Minecraft:
 *  - Fires Level/2 waves of delayed AoE damage with lava/explosion particles.
 *  - Each wave performs all hits at once on nearby entities.
 */
public class MeteorStormSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "meteor_storm");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.5f);

        // RO: MATK × (100 + 40 × level)%
        float damagePerHit = SkillDamageHelper.scaleByMATK(player, 100f + 40f * level);

        // 2 × level hits spread across level waves, 2 hits per wave
        for (int wave = 0; wave < level; wave++) {
            final int waveNum = wave;
            SkillSequencer.schedule(wave * 10, () -> {
                if (!(player.level() instanceof ServerLevel sl)) return;

                // Random offset per wave for AoE scatter effect
                double offsetX = (RANDOM.nextDouble() - 0.5) * 10.0;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 10.0;
                double cx = player.getX() + offsetX;
                double cz = player.getZ() + offsetZ;
                double cy = player.getY();

                // Visual: explosion + lava at impact site
                sl.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.5, cz, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.LAVA, cx, cy + 0.3, cz, 15, 0.5, 0.3, 0.5, 0.05);
                sl.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.8f + waveNum * 0.05f);

                // Hit entities near impact site
                AABB impactBox = new AABB(cx - 2, cy - 0.5, cz - 2, cx + 2, cy + 3, cz + 2);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, impactBox,
                        e -> e != player && e.isAlive());
                for (LivingEntity target : targets) {
                    // 2 hits per wave
                    SkillDamageHelper.dealSkillDamage(target, player.damageSources().magic(), damagePerHit);
                    SkillDamageHelper.dealSkillDamage(target, player.damageSources().magic(), damagePerHit);
                    if (RANDOM.nextFloat() < 0.30f) {
                        target.setSecondsOnFire(3);
                    }
                }
            });
        }
    }
}
