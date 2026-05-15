package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.client.ui.RagnarStatusOverlay;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.init.RagnarEntities;
import com.etema.ragnarmmo.player.stats.PlayerStatsModule;
import net.minecraft.client.Minecraft;
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
    @Mod.EventBusSubscriber(modid = PlayerStatsModule.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            // Default HUD z-order: status, party, hotbar, cast, notifications.
            event.registerAboveAll("ragnar_status", RagnarStatusOverlay.INSTANCE);
            event.registerAboveAll("party_hud", com.etema.ragnarmmo.client.PartyHudOverlay.PARTY_HUD);
            event.registerAboveAll("hotbar_overlay", com.etema.ragnarmmo.client.ui.HotbarOverlay.INSTANCE);
            event.registerAboveAll("ragnar_cast", com.etema.ragnarmmo.client.ui.CastOverlay.INSTANCE);
            event.registerAboveAll("skill_xp", com.etema.ragnarmmo.client.SkillOverlay.HUD_SKILL_XP);
        }

        @SubscribeEvent
        public static void registerTooltipComponents(
                net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(com.etema.ragnarmmo.client.gui.tooltip.CardTooltipData.class,
                    com.etema.ragnarmmo.client.gui.tooltip.ClientCardTooltip::new);
        }

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(RagnarEntities.PORING.get(),
                    com.etema.ragnarmmo.client.render.entity.PoringRenderer::new);
            event.registerEntityRenderer(RagnarEntities.POPORING.get(),
                    com.etema.ragnarmmo.client.render.entity.PoporingRenderer::new);
            event.registerEntityRenderer(RagnarEntities.DROP.get(),
                    com.etema.ragnarmmo.client.render.entity.DropRenderer::new);
            event.registerEntityRenderer(RagnarEntities.MARIN.get(),
                    com.etema.ragnarmmo.client.render.entity.MarinRenderer::new);

            event.registerEntityRenderer(RagnarEntities.LUNATIC.get(),
                    com.etema.ragnarmmo.client.render.entity.LunaticRenderer::new);
            event.registerEntityRenderer(RagnarEntities.FABRE.get(),
                    com.etema.ragnarmmo.client.render.entity.FabreRenderer::new);
            event.registerEntityRenderer(RagnarEntities.PUPA.get(),
                    com.etema.ragnarmmo.client.render.entity.PupaRenderer::new);
            event.registerEntityRenderer(RagnarEntities.MUKA.get(),
                    com.etema.ragnarmmo.client.render.entity.MukaRenderer::new);
            event.registerEntityRenderer(RagnarEntities.CREAMY.get(),
                    com.etema.ragnarmmo.client.render.entity.CreamyRenderer::new);
            event.registerEntityRenderer(RagnarEntities.CREAMY_FEAR.get(),
                    com.etema.ragnarmmo.client.render.entity.CreamyFearRenderer::new);

            event.registerEntityRenderer(RagnarEntities.MAGIC_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.MagicProjectileRenderer::new);
            
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.SOUL_STRIKE_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.SoulStrikeRenderer::new);
            
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.FIRE_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.ICE_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.LIGHTNING_BOLT_PROJECTILE.get(),
                    com.etema.ragnarmmo.client.render.skill.BoltRenderer::new);

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
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.WARP_PORTAL_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.PNEUMA_AOE.get(),
                    com.etema.ragnarmmo.client.render.skill.UniversalSkillRenderer::new);
            event.registerEntityRenderer(com.etema.ragnarmmo.common.init.RagnarEntities.STATUS_OVERLAY.get(),
                    com.etema.ragnarmmo.client.render.entity.StatusOverlayRenderer::new);
        }

    }

    /**
     * FORGE bus events (client commands, input events, etc.)
     */
    @Mod.EventBusSubscriber(modid = PlayerStatsModule.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            ClientCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiOverlayEvent.Pre event) {
            var overlayId = event.getOverlay().id();
            boolean replaceSurvivalBars = shouldReplaceVanillaSurvivalBars();

            if (replaceSurvivalBars && overlayId.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
                event.setCanceled(true);
            } else if (replaceSurvivalBars && overlayId.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
                event.setCanceled(true);
            } else if (overlayId.equals(VanillaGuiOverlay.AIR_LEVEL.id())) {
                // Move air bubbles up to avoid overlapping with the skill hotbar
                event.getGuiGraphics().pose().pushPose();
                event.getGuiGraphics().pose().translate(0, -32, 0);
            }
        }

        @SubscribeEvent
        public static void onRenderGuiLayerPost(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())) {
                event.getGuiGraphics().pose().popPose();
            }
        }

        private static boolean shouldReplaceVanillaSurvivalBars() {
            Minecraft mc = Minecraft.getInstance();
            return RagnarConfigs.CLIENT.hud.replaceVanillaSurvivalBars.get()
                    && RagnarConfigs.CLIENT.hud.enabled.get()
                    && RagnarConfigs.CLIENT.hud.status.enabled.get()
                    && mc.player != null
                    && !mc.options.hideGui
                    && !mc.player.isSpectator();
        }
    }
}
