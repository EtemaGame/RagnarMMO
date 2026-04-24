package com.etema.ragnarmmo.mobs.event;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.network.SyncMobProfilePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class MobTrackingHandler {

    private MobTrackingHandler() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity target
                && !(target instanceof Player)
                && event.getEntity() instanceof ServerPlayer player) {

            SyncMobProfilePacket.fromEntity(target)
                    .ifPresent(packet -> Network.sendToPlayer(player, packet));
        }
    }
}






