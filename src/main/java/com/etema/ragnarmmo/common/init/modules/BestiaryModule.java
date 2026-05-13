package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.bestiary.data.BestiaryRegistry;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BestiaryModule {
    private BestiaryModule() {
    }

    public static void init(IEventBus modBus) {
        Network.registerBestiaryPackets();
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static final class Events {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                BestiaryRegistry.getInstance().syncToPlayer(player);
            }
        }
    }
}
