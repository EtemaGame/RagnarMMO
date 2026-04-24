package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.spawn.MobSpawnHandler;
import com.etema.ragnarmmo.mobs.util.AttributeLimitHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class MobsModule {
    private MobsModule() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(MobsModule::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(new MobSpawnHandler());
        Network.registerMobPackets();
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        AttributeLimitHelper.onCommonSetup(event);
    }
}
