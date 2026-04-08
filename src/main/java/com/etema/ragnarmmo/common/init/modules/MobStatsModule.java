package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;
import com.etema.ragnarmmo.system.mobstats.events.MobSpawnHandler;
import com.etema.ragnarmmo.system.mobstats.util.AttributeLimitHelper;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class MobStatsModule {
    private MobStatsModule() {
    }

    public static void init(IEventBus modBus) {
        // Register configuration
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.etema.ragnarmmo.system.mobstats.config.MobConfig.SPEC,
                "ragnarmmo-mobstats.toml");

        modBus.addListener(MobStatsModule::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(new MobSpawnHandler());

        Network.registerMobStatsPackets();
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SpeciesConfig::init);
        AttributeLimitHelper.onCommonSetup(event);
    }
}
