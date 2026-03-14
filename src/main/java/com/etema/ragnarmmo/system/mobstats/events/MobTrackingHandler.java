package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobStatsPacket;
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
        if (event.getTarget() instanceof LivingEntity target && !(target instanceof Player)) {
            MobStatsProvider.get(target).ifPresent(stats -> {
                if (stats.isInitialized()) {
                    Network.sendTrackingEntityAndSelf(target, new SyncMobStatsPacket(target.getId(), stats));
                }
            });
        }
    }
}






