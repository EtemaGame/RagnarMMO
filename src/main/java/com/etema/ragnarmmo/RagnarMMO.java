package com.etema.ragnarmmo;

import com.etema.ragnarmmo.common.init.RagnarCommand;
import com.etema.ragnarmmo.common.init.modules.BarModule;
import com.etema.ragnarmmo.common.init.modules.CoreModule;
import com.etema.ragnarmmo.common.init.modules.CombatModule;
import com.etema.ragnarmmo.common.init.modules.LifeSkillsModule;
import com.etema.ragnarmmo.common.init.modules.MobsModule;
import com.etema.ragnarmmo.common.init.modules.SkillsModule;
import com.etema.ragnarmmo.common.init.modules.StatsModule;
import com.etema.ragnarmmo.items.ItemsModule;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RagnarMMO.MODID)
public class RagnarMMO {
    public static final String MODID = "ragnarmmo";
    public static final String VERSION = computeVersion();
    public static final Logger LOGGER = LoggerFactory.getLogger(RagnarMMO.class);

    private static String computeVersion() {
        try {
            return net.minecraftforge.fml.ModList.get()
                    .getModContainerById(MODID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (Exception e) {
            return "test";
        }
    }

    @SuppressWarnings("removal")
    public RagnarMMO() {
        LOGGER.info("=== Initializing RagnarMMO v{} ===", VERSION);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        CoreModule.init(modBus);
        CombatModule.init(modBus);
        StatsModule.init(modBus);
        MobsModule.init(modBus);
        SkillsModule.init(modBus);
        LifeSkillsModule.init(modBus);
        ItemsModule.init(modBus);
        com.etema.ragnarmmo.common.init.modules.EconomyModule.init(modBus);

        BarModule.init();

        MinecraftForge.EVENT_BUS.register(this);

        com.etema.ragnarmmo.common.init.RagnarCreativeTabs.register(modBus);

        LOGGER.info("=== RagnarMMO initialized successfully ===");
    }

    @SubscribeEvent
    public void onRegisterCommands(final RegisterCommandsEvent event) {
        RagnarCommand.register(event.getDispatcher());
        LOGGER.info("Registered RagnarMMO commands");
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        LOGGER.info("=== RagnarMMO is ready on server ===");
    }

    @SubscribeEvent
    public void onServerStopping(final ServerStoppingEvent event) {
        LOGGER.info("RagnarMMO shutting down");
    }

    public static String getVersion() {
        return VERSION;
    }
}
