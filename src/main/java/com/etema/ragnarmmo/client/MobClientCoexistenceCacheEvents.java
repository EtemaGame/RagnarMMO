package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Clears the minimal coexistence client projection when entities or sessions go away.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobClientCoexistenceCacheEvents {

    private MobClientCoexistenceCacheEvents() {
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        MobClientCoexistenceCache.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MobClientCoexistenceCache.clear();
    }
}
