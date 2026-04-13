package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Module initialization for the RO Items system.
 * Registers items and sets up basic services.
 * Unified config is now handled via RagnarConfigs.
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

        // Register items
        ZenyItems.register(modBus);
        UtilityItems.register(modBus);
        RagnarWeaponItems.register(modBus);
        com.etema.ragnarmmo.system.loot.cards.RagnarCardItems.register(modBus);

        RagnarMMO.LOGGER.info("RO Items module initialized");
    }
}
