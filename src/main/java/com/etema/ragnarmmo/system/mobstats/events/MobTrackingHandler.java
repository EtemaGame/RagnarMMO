package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobCoexistenceViewPacket;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobStatsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMobStats.MOD_ID)
public final class MobTrackingHandler {

    private MobTrackingHandler() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity target
                && !(target instanceof Player)
                && event.getEntity() instanceof ServerPlayer player) {
            
            com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority authority = com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthorityResolver.classify(target);

            if (authority != com.etema.ragnarmmo.common.api.mobs.MobRuntimeAuthority.STRICT_NEW_AUTHORITY) {
                SyncMobStatsPacket.fromEntity(target)
                        .ifPresent(packet -> Network.sendTrackingEntityAndSelf(target, packet));
            } else {
                com.etema.ragnarmmo.common.debug.RagnarDebugLog.migration("Sync: Skipping legacy stats packet for STRICT mob. entity={}",
                        com.etema.ragnarmmo.common.debug.RagnarDebugLog.entityLabel(target));
            }

            SyncMobCoexistenceViewPacket.fromEntity(target)
                    .ifPresent(packet -> Network.sendToPlayer(player, packet));
        }
    }
}






