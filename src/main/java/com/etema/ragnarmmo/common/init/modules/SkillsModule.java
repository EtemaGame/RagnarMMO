package com.etema.ragnarmmo.common.init.modules;

import net.minecraftforge.eventbus.api.IEventBus;

public final class SkillsModule {
    private SkillsModule() {}

    public static void init(IEventBus modBus) {
        com.etema.ragnarmmo.items.loot.RagnarLootModifiers.register(modBus);
    }
}






