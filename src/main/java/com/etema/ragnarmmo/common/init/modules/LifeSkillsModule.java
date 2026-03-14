package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Module for initializing Life Skills system components.
 * - Point-based progression (separate from combat XP)
 * - Anti-exploit block tracking
 * - JSON data-driven point sources
 * - Perk system every 10 levels
 */
public final class LifeSkillsModule {
    private LifeSkillsModule() {}

    public static void init(IEventBus modBus) {
        // Register network packets for life skills
        Network.registerLifeSkillPackets();

        RagnarMMO.LOGGER.info("Life Skills module initialized");
    }
}
