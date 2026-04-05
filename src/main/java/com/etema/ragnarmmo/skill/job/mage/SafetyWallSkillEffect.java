package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Safety Wall — Active (Holy barrier)
 * RO: Creates an invincible barrier on the ground that absorbs a number of hits.
 *     Hit absorptions = (skill level + 1). Lasts 5 + level seconds.
 *
 * Minecraft:
 *  - Grants the caster a temporary hit-absorption barrier tracked via PersistentData.
 *    "ragnar_safetywall_hits" = remaining hits absorbed.
 *    "ragnar_safetywall_until" = expiry game-time.
 *  - While active, physical attacks against the player are absorbed (cancelled from LivingHurtEvent).
 *  - Magic attacks bypass Safety Wall (consistent with RO behaviour).
 *  - Visual: TOTEM particles around the caster's feet + holy chime sound.
 *  - Absorption is handled in SafetyWallEvents.
 */
public class SafetyWallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "safety_wall");
    public static final String SW_HITS_TAG  = "ragnar_safetywall_hits";
    public static final String SW_UNTIL_TAG = "ragnar_safetywall_until";

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public int getCastTime(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 70;
            case 3 -> 60;
            case 4 -> 50;
            case 5 -> 40;
            case 6 -> 30;
            case 7, 8, 9, 10 -> 20;
            default -> 80;
        };
    }

    @Override
    public int getResourceCost(int level, int defaultCost) {
        if (level >= 7) return 40;
        if (level >= 4) return 35;
        return 30;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int maxHits = level + 1;            // Level 1 = 2 hits, Level 10 = 11 hits
        int durationTicks = (5 + level) * 20; // 6-15 seconds

        player.getPersistentData().putInt(SW_HITS_TAG,  maxHits);
        player.getPersistentData().putLong(SW_UNTIL_TAG, player.level().getGameTime() + durationTicks);

        // Sounds: holy shimmer
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.2f);

        // Particles: protective aura column rising from the feet
        if (player.level() instanceof ServerLevel sl) {
            // Ground ring
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double px = player.getX() + Math.cos(angle) * 1.2;
                double pz = player.getZ() + Math.sin(angle) * 1.2;
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, player.getY() + 0.1, pz, 3, 0, 0.4, 0, 0.02);
                sl.sendParticles(ParticleTypes.ENCHANT, px, player.getY() + 0.1, pz, 2, 0, 0.3, 0, 0.01);
            }
            // Upward sparkles
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    20, 0.4, 1.0, 0.4, 0.04);
        }
    }
}
