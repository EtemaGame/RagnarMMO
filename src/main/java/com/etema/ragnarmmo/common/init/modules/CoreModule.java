package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class CoreModule {
    private CoreModule() {
    }

    @SuppressWarnings("removal") // ModLoadingContext.get() deprecated in 1.20.4+, valid for 1.20.1
    public static void init(IEventBus modBus) {
        RagnarAttributes.register(modBus);
        com.etema.ragnarmmo.common.init.RagnarEntities.register(modBus);

        // Register RO item packets for rule syncing
        Network.registerRoItemPackets();
        Network.registerAchievementPackets();

        // Register unified configs (only 2 files)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RagnarConfigs.CLIENT_SPEC,
                "ragnarmmo-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RagnarConfigs.SERVER_SPEC,
                "ragnarmmo-common.toml");
    }
}
