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

    @SuppressWarnings("removal")
    public static void init(IEventBus modBus) {
        RagnarAttributes.register(modBus);
        com.etema.ragnarmmo.common.init.RagnarEntities.register(modBus);
        com.etema.ragnarmmo.common.init.RagnarSounds.register(modBus);
        com.etema.ragnarmmo.common.init.RagnarMobEffects.register(modBus);
        com.etema.ragnarmmo.common.init.RagnarParticles.register(modBus);

        Network.registerRoItemPackets();
        Network.registerAchievementPackets();
        Network.registerSkillEffectPackets();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RagnarConfigs.CLIENT_SPEC,
                "ragnarmmo-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RagnarConfigs.SERVER_SPEC,
                "ragnarmmo-server.toml");
    }
}
