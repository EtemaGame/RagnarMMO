package com.etema.ragnarmmo.common.api.mobs.runtime.integration;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobCoexistenceViewPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sends the minimal new-source coexistence projection once a covered manual mob is present server-side.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class ManualMobProfileCoexistenceSyncBridge {

    private ManualMobProfileCoexistenceSyncBridge() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) {
            return;
        }

        SyncMobCoexistenceViewPacket.fromEntity(entity)
                .ifPresent(packet -> Network.sendTrackingEntityAndSelf(entity, packet));
    }
}
