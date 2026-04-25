package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import com.etema.ragnarmmo.player.stats.compute.StatComputer;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraftforge.eventbus.api.IEventBus;

public final class StatsModule {
    private StatsModule() {
    }

    public static void init(IEventBus modBus) {
        // Config registration moved to CoreModule (unified RagnarConfigs)

        Network.registerStatsPackets();
        Network.registerPartyPackets();

        RagnarCoreAPI.registerAccessor(player -> player.getCapability(PlayerStatsProvider.CAP).resolve());
        RagnarCoreAPI.registerComputeFunction(StatComputer::compute);
    }
}
