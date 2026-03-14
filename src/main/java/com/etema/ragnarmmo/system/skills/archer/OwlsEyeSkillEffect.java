package com.etema.ragnarmmo.system.skills.archer;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * Owl's Eye — Passive
 * RO: Permanently adds +level DEX (accuracy/ranged attack power).
 *
 * Fixed: was using non-existent RagnarCoreAPI.get(), now uses PlayerStatsProvider.CAP.
 * Uses onPeriodicTick to keep the bonus set (idempotent: only writes if changed).
 */
public class OwlsEyeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "owls_eye");

    @Override
    public ResourceLocation getSkillId() { return ID; }

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
