package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;

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
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Duration: 10s + 3s per level (matches RO 10-37s range)
        int durationTicks = (10 + (level * 3)) * 20;

        // Max knockback hits blocked: 7 (RO constant)
        int maxHits = 7;

        // Store endure state in PersistentData
        player.getPersistentData().putLong(ENDURE_TAG, player.level().getGameTime() + durationTicks);
        player.getPersistentData().putInt(ENDURE_HITS_TAG, maxHits);

        // Visual: Resistance as status effect to show the player they're buffed
        // Amplifier scales with level for the MDEF bonus (cosmetic in this context)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks,
                Math.min(level / 3, 3), false, true, true));

        // Sounds & Particles
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f);

        if (player.level() instanceof ServerLevel serverLevel) {
            // Aura of particles around the player
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.8, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    10, 0.3, 0.5, 0.3, 0.05);
        }
    }
}
