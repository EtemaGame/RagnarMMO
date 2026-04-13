package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.system.economy.ZenyDropEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EconomyModule {
    private EconomyModule() {}

    public static void init(IEventBus modBus) {
        // Register event handler
        MinecraftForge.EVENT_BUS.register(new ZenyDropEventHandler());
        
        // Register Network Packets
        com.etema.ragnarmmo.common.net.Network.registerEconomyPackets();
    }
}
