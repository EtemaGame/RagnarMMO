package com.etema.ragnarmmo.system.skills.archer;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
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
 *     Also reveals all nearby hidden/cloaked enemies (same as Sight range).
 *
 * Fixed:
 *  - Replaced non-existent RagnarCoreAPI.get() with PlayerStatsProvider.CAP.
 *  - Bonus is now stored in PersistentData and removed when the buff expires.
 *    Key: "ragnar_conc_dex_bonus", "ragnar_conc_agi_bonus", "ragnar_conc_until".
 *  - Visual: ENCHANT ring + MOVEMENT_SPEED effect for duration.
 */
public class ImproveConcentrationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_concentration");
    public static final String CONC_UNTIL_TAG = "ragnar_conc_until";
    public static final String CONC_DEX_TAG   = "ragnar_conc_dex_bonus";
    public static final String CONC_AGI_TAG   = "ragnar_conc_agi_bonus";

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int durationTicks = (40 + 20 * level) * 20;

        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            // Remove previous bonus if still active
            if (player.getPersistentData().contains(CONC_UNTIL_TAG)) {
                stats.addBonus(StatKeys.DEX, -player.getPersistentData().getInt(CONC_DEX_TAG));
                stats.addBonus(StatKeys.AGI, -player.getPersistentData().getInt(CONC_AGI_TAG));
            }

            float pct = (2 + level) / 100.0f;
            int dexBonus = Math.max(1, (int)(stats.getDEX() * pct));
            int agiBonus = Math.max(1, (int)(stats.getAGI() * pct));

            stats.addBonus(StatKeys.DEX, dexBonus);
            stats.addBonus(StatKeys.AGI, agiBonus);

            // Store for expiry removal
            player.getPersistentData().putLong(CONC_UNTIL_TAG, player.level().getGameTime() + durationTicks);
            player.getPersistentData().putInt(CONC_DEX_TAG, dexBonus);
            player.getPersistentData().putInt(CONC_AGI_TAG, agiBonus);
        });

        // Visual MC effect: Speed as AGI proxy
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 0, false, false, true));

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
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.3f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    30, 0.6, 1.0, 0.6, 0.08);
        }
    }
}
