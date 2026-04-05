package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
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
 * Improve Concentration — Active
 * RO: Temporarily raises DEX and AGI by (2 + level)% for (40 + 20*level) seconds.
 *     Also reveals all nearby hidden/cloaked enemies.
 */
public class ImproveConcentrationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_concentration");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int durationTicks = (40 + 20 * level) * 20;

        // Apply MobEffect (Handles AGI and DEX % bonuses via AttributeModifiers)
        player.addEffect(new MobEffectInstance(RagnarMobEffects.IMPROVE_CONCENTRATION.get(), durationTicks, level - 1));

        // Reveal nearby hidden entities (same as Mage Sight)
        AABB area = player.getBoundingBox().inflate(7.0);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());
        for (LivingEntity e : nearby) {
            if (e.getPersistentData().contains("ragnar_cloaked_until")) {
                e.getPersistentData().remove("ragnar_cloaked_until");
                e.setInvisible(false);
            }
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, false));
        }

        // Sounds and particles
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                RagnarSounds.CONCENTRATION.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    30, 0.6, 1.0, 0.6, 0.08);
        }
    }
}
