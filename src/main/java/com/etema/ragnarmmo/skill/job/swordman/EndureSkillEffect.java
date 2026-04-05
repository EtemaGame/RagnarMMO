package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Endure — Active
 * RO: Grants immunity to knockback for a number of hits or until duration expires.
 *     Also increases MDEF by +1 to +10 per level.
 *
 * Minecraft:
 *  - Grants Resistance 1 (cosmetic/minor DR) for the duration.
 *  - Knockback immunity is tracked via PersistentData tag "ragnar_endure_until" on the player.
 *  - LivingKnockBackEvent is handled in EndureEvents (same pattern as StunEvents).
 *  - Duration: 10s at level 1 → 37s at level 10 (RO formula).
 */
public class EndureSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "endure");
    public static final String ENDURE_TAG = "ragnar_endure_until";
    public static final String ENDURE_HITS_TAG = "ragnar_endure_hits";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(LivingEntity user, int level) {
        if (level <= 0) return;


        user.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        int durationTicks = (10 + ((level - 1) * 3)) * 20;

        // Max knockback hits blocked: 7 (RO constant)
        int maxHits = 7;

        // Store endure state in PersistentData
        user.getPersistentData().putLong(ENDURE_TAG, user.level().getGameTime() + durationTicks);
        user.getPersistentData().putInt(ENDURE_HITS_TAG, maxHits);

        // Visual: Custom Endure MobEffect to show the icon
        user.addEffect(new MobEffectInstance(RagnarMobEffects.ENDURE.get(), durationTicks,
                Math.max(0, level - 1), false, true, true));

        // Sounds & Particles
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                RagnarSounds.ENDURE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (user.level() instanceof ServerLevel serverLevel) {
            // Aura of particles around the user
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    user.getX(), user.getY() + 1.0, user.getZ(),
                    25, 0.5, 0.8, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    user.getX(), user.getY() + 0.5, user.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
            SkillVisualFx.spawnRing(serverLevel, user.position(), 1.05, 0.15, ParticleTypes.ENCHANTED_HIT, 10);
            SkillVisualFx.spawnRing(serverLevel, user.position(), 0.7, 1.2, ParticleTypes.GLOW, 6);
            
            // Persistent visual: small flashes for 2 seconds
            for (int i = 0; i < 5; i++) {
                final int pulse = i;
                com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(i * 10, () -> {
                    if (user.isAlive()) {
                        serverLevel.sendParticles(ParticleTypes.GLOW, 
                            user.getX(), user.getY() + 1.0, user.getZ(), 
                            5, 0.4, 0.6, 0.4, 0.01);
                        SkillVisualFx.spawnRing(serverLevel, user.position(), 0.95, 0.2 + (pulse * 0.08),
                                ParticleTypes.GLOW, 8);
                    }
                });
            }
        }
    }
}
