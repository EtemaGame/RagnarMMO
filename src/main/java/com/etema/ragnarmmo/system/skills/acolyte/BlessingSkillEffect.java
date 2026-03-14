package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class BlessingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "blessing");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Blessing: Temporarily increases STR, INT, and DEX of the target by 1 per
        // level.
        // Duration: 60 + 20 * level seconds.
        // For Minecraft, we'll apply it to the player who cast it, or their immediate
        // look target.
        // Since we don't have a robust cross-entity temporary buff system built in
        // here,
        // we will apply it as a bonus to the caster for simplicity, or we can use
        // attributes directly if we want.

        int bonusAmount = level;

        // Apply to self
        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            stats.addBonus(StatKeys.STR, bonusAmount);
            stats.addBonus(StatKeys.DEX, bonusAmount);
            stats.addBonus(StatKeys.INT, bonusAmount);
            // In a full implementation, we'd schedule removal.
        });

        // SFX and VFX
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
