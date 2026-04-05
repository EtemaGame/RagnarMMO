package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.Set;

/**
 * Owl's Eye — Passive
 * RO: Permanently adds +level DEX (accuracy/ranged attack power).
 *
 * Fixed: was using non-existent RagnarCoreAPI.get(), now uses PlayerStatsProvider.CAP.
 * Uses onPeriodicTick to keep the bonus set (idempotent: only writes if changed).
 */
public class OwlsEyeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "owls_eye");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK);
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            int currentBonus = stats.getBonus(StatKeys.DEX);
            if (currentBonus != level) {
                stats.setBonus(StatKeys.DEX, level);
            }
        });
    }
}
