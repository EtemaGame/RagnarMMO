package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class IncreaseAgiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "increase_agi");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Increase AGI: Temporarily increases AGI by (2 + level) and increases movement
        // speed.
        // Duration: 40 + 20 * level seconds.
        // Applied to self for now.

        int agiBonus = 2 + level;
        int durationTicks = (40 + 20 * level) * 20;

        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            stats.addBonus(StatKeys.AGI, agiBonus);
        });

        // Movement speed buff via vanilla effect
        // Level 1 speed for skill levels 1-5, Level 2 for 6-10
        int speedLevel = level >= 6 ? 1 : 0;
        player.addEffect(
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, speedLevel, false, false, true));

        // SFX and VFX
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 15, 0.3,
                    0.1, 0.3, 0.05);
        }
    }
}
