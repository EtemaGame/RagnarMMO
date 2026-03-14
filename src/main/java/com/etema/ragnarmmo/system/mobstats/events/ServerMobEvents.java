package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.MobHurtPacket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side event handlers for mob-related network sync.
 * Sends packets to clients when mobs are hurt so health bars can be displayed.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerMobEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        // Only process on server side
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        // Don't send packets for players (they have their own health display)
        if (entity instanceof Player) {
            return;
        }

        // Skip if damage was cancelled
        if (event.isCanceled() || event.getAmount() <= 0) {
            return;
        }

        // Send to all players tracking this entity
        if (entity.level() instanceof ServerLevel serverLevel) {
            MobHurtPacket packet = new MobHurtPacket(entity.getId());
            Network.sendToTrackingEntity(entity, packet);
        }
    }
}
