package com.etema.ragnarmmo.items.hook;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.items.runtime.RoAttributeApplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Re-validates equipment attributes when player state changes (login, dimension
 * change).
 * Ensures that attribute modifiers are correctly applied or restored.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class RoRevalidateEquipmentHook {

    private RoRevalidateEquipmentHook() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoAttributeApplier.refreshAllSlots(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoAttributeApplier.refreshAllSlots(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoAttributeApplier.refreshAllSlots(player);
        }
    }
}
