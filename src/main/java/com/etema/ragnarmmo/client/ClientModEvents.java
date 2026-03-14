package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.client.command.ClientHudCommands;
import com.etema.ragnarmmo.client.ui.RagnarStatusOverlay;
import com.etema.ragnarmmo.system.stats.RagnarStats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side mod events for RagnarMMO.
 * Contains separate inner classes for MOD bus and FORGE bus events.
 */
public class ClientModEvents {

    /**
     * MOD bus events (overlay registration, keybinds, etc.)
     */
    @Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            // Register our custom HUD overlay above all vanilla overlays
            event.registerAboveAll("ragnar_status", RagnarStatusOverlay.INSTANCE);
        }

        @SubscribeEvent
        public static void registerTooltipComponents(
                net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(com.etema.ragnarmmo.client.gui.tooltip.CardTooltipData.class,
                    com.etema.ragnarmmo.client.gui.tooltip.ClientCardTooltip::new);
        }
    }

    /**
     * FORGE bus events (client commands, input events, etc.)
     */
    @Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            ClientHudCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
                event.setCanceled(true);
            } else if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
                event.setCanceled(true);
            } else if (event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())) {
                // Move air bubbles up to avoid overlapping with the skill hotbar
                event.getGuiGraphics().pose().pushPose();
                event.getGuiGraphics().pose().translate(0, -25, 0);
            }
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPost(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())) {
                event.getGuiGraphics().pose().popPose();
            }
        }
    }
}
