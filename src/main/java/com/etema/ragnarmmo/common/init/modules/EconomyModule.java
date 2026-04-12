package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.system.economy.ZenyDropEventHandler;
import com.etema.ragnarmmo.system.economy.config.ZenyConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class EconomyModule {
    private EconomyModule() {}

    public static void init(IEventBus modBus) {
        // Register configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ZenyConfig.SPEC, "ragnarmmo-economy.toml");

        // Register event handler
        MinecraftForge.EVENT_BUS.register(new ZenyDropEventHandler());
        
        // Register Network Packets
        com.etema.ragnarmmo.common.net.Network.registerEconomyPackets();
    }
}
