package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.client.command.ClientHudCommands;
import com.etema.ragnarmmo.client.ui.RagnarStatusOverlay;
import com.etema.ragnarmmo.system.stats.RagnarStats;
import net.minecraft.resources.ResourceLocation;
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
        public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // Register Acolyte Visuals
                com.etema.ragnarmmo.client.render.skill.SkillVisualsRegistry.register(
                    new ResourceLocation("ragnarmmo", "heal"),
                    com.etema.ragnarmmo.client.render.skill.SkillVisuals.builder(new ResourceLocation("ragnarmmo", "textures/entity/magic/soul_strike_1.png"))
                        .columns(4).rows(1).size(1.5f).billboard(true).animationFPS(10).build()
                );
                
                com.etema.ragnarmmo.client.render.skill.SkillVisualsRegistry.register(
                    new ResourceLocation("ragnarmmo", "blessing"),
                    com.etema.ragnarmmo.client.render.skill.SkillVisuals.builder(new ResourceLocation("ragnarmmo", "textures/entity/magic/soul_strike_1.png"))
                        .columns(2).rows(2).size(1.2f).billboard(true).animationFPS(8).build()
                );

                com.etema.ragnarmmo.client.render.skill.SkillVisualsRegistry.register(
                    new ResourceLocation("ragnarmmo", "sanctuary"),
                    com.etema.ragnarmmo.client.render.skill.SkillVisuals.builder(new ResourceLocation("ragnarmmo", "textures/entity/magic/sanctuary_1.png"))
                        .type(com.etema.ragnarmmo.client.render.skill.SkillVisuals.VisualType.SPRITE)
                        .size(4.0f).animationFPS(5).build()
                );

                // Register Mage Visuals
                com.etema.ragnarmmo.client.render.skill.SkillVisualsRegistry.register(
                    new ResourceLocation("ragnarmmo", "napalm_beat"),
                    com.etema.ragnarmmo.client.render.skill.SkillVisuals.builder(new ResourceLocation("ragnarmmo", "textures/entity/magic/soul_strike_1.png"))
                        .columns(1).rows(1).size(2.0f).animationFPS(10).build()
                );
                
                com.etema.ragnarmmo.client.render.skill.SkillVisualsRegistry.register(
                    new ResourceLocation("ragnarmmo", "heavens_drive"),
                    com.etema.ragnarmmo.client.render.skill.SkillVisuals.builder(new ResourceLocation("ragnarmmo", "textures/entity/magic/heavens_drive_1.png"))
                        .type(com.etema.ragnarmmo.client.render.skill.SkillVisuals.VisualType.BLOCK)
                        .block(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState())
                        .size(1.0f).build()
                );
            });
        }

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

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.MAGIC_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.SOUL_STRIKE_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            
            // Shared Bolt Renderer (Keeping BoltRenderer for now as it might have specific logic, but can be migrated later)
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.FIRE_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.ICE_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.LIGHTNING_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);

            // AOE Entities (Now handled by UniversalSkillRenderer)
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.NAPALM_BEAT_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.HEAVENS_DRIVE_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.STORM_GUST_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.FIRE_WALL_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.SANCTUARY_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
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
