package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.Set;

/**
 * Increase SP Recovery — Passive
 * RO: While standing still, recovers resource every 10 seconds.
 * Amount: (3 x level) + (0.2% of max resource x level).
 *
 * Minecraft:
 *  - Reuses the mod's active resource (Mana for magical jobs, SP for physical jobs).
 *  - Only triggers if the player is standing still on the ground.
 */
public class IncreaseSpRecoverySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "increase_sp_recovery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK);
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        var defOpt = SkillRegistry.get(ID);
        int intervalTicks = defOpt
                .map(def -> def.getLevelInt("interval_ticks", level, 200))
                .orElse(200);
        if (player.tickCount % Math.max(1, intervalTicks) != 0) return;

        if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || !player.onGround()) return;

        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            double max = stats.getMaxResource();
            if (stats.getCurrentResource() < max) {
                double flat = defOpt
                        .map(def -> def.getLevelDouble("resource_recovery_flat", level, level * 3.0D))
                        .orElse(level * 3.0D);
                double maxRatio = defOpt
                        .map(def -> def.getLevelDouble("resource_recovery_max_ratio", level, 0.002D * level))
                        .orElse(0.002D * level);
                double bonus = flat + (max * maxRatio);
                stats.addResource(bonus);
            }
        });
    }
}
