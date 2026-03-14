package com.etema.ragnarmmo.system.skills.wizard;

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

/**
 * Frost Nova — Active (Water/Ice AoE)
 * RO: Freezes enemies in a small area around the caster. 30% freeze chance.
 *
 * Fixed from original: missing freeze proc implementation.
 */
public class FrostNovaSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "frost_nova");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        AABB area = player.getBoundingBox().inflate(3.0 + (level * 0.5));
        if (!(player.level() instanceof ServerLevel sl)) return;

        // Particles: icy ring burst from player
        sl.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1, player.getZ(),
                120, 2.5, 0.8, 2.5, 0.08);
        sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                player.getX(), player.getY() + 0.5, player.getZ(),
                30, 1.5, 0.2, 1.5, 0.12);
        sl.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        sl.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);

        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().magic(), 4.0f + (level * 2.0f));

            // Always apply Slowness 2 (chilled)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));

            // 30% + 5%/level freeze proc
            float freezeChance = 0.30f + (0.05f * level);
            if (player.getRandom().nextFloat() < freezeChance) {
                int freezeTicks = (3 + level) * 20;
                // Full freeze: Slowness 10 (immobile) + visual frost
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, 10, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, freezeTicks, 1, false, true, false));
                target.setTicksFrozen(freezeTicks);

                // Extra particle burst on frozen target
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 1, target.getZ(),
                        25, 0.4, 0.5, 0.4, 0.03);
            }
        }
    }
}
