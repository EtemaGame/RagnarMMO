package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Initializes the server-authoritative combat foundation.
 */
public final class CombatModule {
    private CombatModule() {
    }

    public static void init(IEventBus modBus) {
        Network.registerCombatPackets();
        RagnarMMO.LOGGER.info("Combat architecture foundation initialized");
    }
}
