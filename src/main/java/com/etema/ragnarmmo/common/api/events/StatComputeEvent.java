package com.etema.ragnarmmo.common.api.events;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class StatComputeEvent extends Event {
    private final Player player;
    private final IPlayerStats stats;
    private final DerivedStats derived;

    public StatComputeEvent(Player player, IPlayerStats stats, DerivedStats derived) {
        this.player = player;
        this.stats = stats;
        this.derived = derived;
    }

    public Player getPlayer() {
        return player;
    }

    public IPlayerStats getStats() {
        return stats;
    }

    public DerivedStats getDerived() {
        return derived;
    }
}






