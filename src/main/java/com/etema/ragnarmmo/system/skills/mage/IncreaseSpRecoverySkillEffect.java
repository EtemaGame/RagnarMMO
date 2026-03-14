package com.etema.ragnarmmo.system.skills.mage;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * Increase SP Recovery — Passive
 * RO: Increases SP regen rate. SP regen = (3 × level) + (0.2% MaxSP × level) every 10s.
 *
 * Minecraft:
 *  - On every tick (via onPeriodicTick every second), awards extra Mana/SP
 *    via the mod's PlayerStats capability.
 *  - Magical jobs (Mage, Wizard): increases Mana recovery.
 *  - Physical jobs: increases SP recovery.
 *  - Amount: +level units per second (e.g. Level 5 = +5 Mana/SP per second).
 */
public class IncreaseSpRecoverySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "increase_sp_recovery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;

        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            // addResource() already dispatches to addMana or addSP based on job type
            double bonus = level; // +level units/second
            double max = stats.getMaxResource();
            if (stats.getCurrentResource() < max) {
                stats.addResource(bonus);
                stats.markDirty();
            }
        });
    }
}
