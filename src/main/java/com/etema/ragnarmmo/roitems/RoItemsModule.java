package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.config.RoItemsConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Module initialization for the RO Items system.
 * Registers configuration and sets up the system.
 *
 * All event handlers are auto-registered via @EventBusSubscriber annotations.
 */
public final class RoItemsModule {

    private RoItemsModule() {}

    /**
     * Initialize the RO Items module.
     * Should be called from RagnarMMO main class during mod initialization.
     *
     * @param modBus the mod event bus
     */
    public static void init(IEventBus modBus) {
        RagnarMMO.LOGGER.info("Initializing RO Items module...");

        // Register configuration
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                RoItemsConfig.SPEC,
                "ragnarmmo-roitems.toml"
        );

        // Note: All event handlers use @EventBusSubscriber and are auto-registered
        // - RoItemRuleLoader.Events (AddReloadListenerEvent)
        // - RoItemAttributeHook (PlayerTickEvent)
        // - RoLoginSyncHook (EntityJoinLevelEvent, PlayerRespawnEvent, etc.)
        // - RoEquipRestrictionHook (PlayerInteractEvent.RightClickItem)
        // - RoCombatRestrictionHook (LivingHurtEvent)
        // - RoTooltipHook (ItemTooltipEvent, client only)

        RagnarMMO.LOGGER.info("RO Items module initialized");
    }
}
