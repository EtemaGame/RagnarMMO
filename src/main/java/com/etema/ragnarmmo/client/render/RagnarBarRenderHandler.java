package com.etema.ragnarmmo.client.render;

import com.etema.ragnarmmo.client.hud.EntityStatResolver;
import com.etema.ragnarmmo.client.hud.RagnarIntegrationHandler;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Renderiza una barra de vida y etiquetas sobre mobs o jugadores
 * con integración automática a RagnarStats y RagnarMobStats.
 */
@Mod.EventBusSubscriber(modid = com.etema.ragnarmmo.RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RagnarBarRenderHandler {

    private static final double MAX_DISTANCE = 40.0D;
    private static final float SCALE = 0.82F;
    private static final int BAR_WIDTH = 46;
    private static final int BAR_HEIGHT = 4;
    private static final long DISPLAY_TIME_MS = 2000;

    private static final Map<LivingEntity, Long> lastHitTime = new WeakHashMap<>();

    /**
     * Called from MobHurtPacket handler to mark an entity as recently hurt.
     */
    public static void markEntityHurt(LivingEntity entity) {
        if (entity != null) {
            lastHitTime.put(entity, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity != null && entity.level().isClientSide()) {
            lastHitTime.put(entity, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            // Hide vanilla nametag to prevent duplication with our custom HUD
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<LivingEntity, ?> e) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = e.getEntity();

        Player player = mc.player;
        if (player == null || mc.options.hideGui)
            return;

        if (mc.screen instanceof com.etema.ragnarmmo.client.ui.SkillsScreen) {
            return;
        }

        if (!entity.isAlive() || entity.isInvisible())
            return;
        if (player.distanceTo(entity) > MAX_DISTANCE)
            return;
        if (!(entity instanceof Player) && isRepresentedByTargetFrame(mc, entity))
            return;

        // === Datos base ===
        EntityStatResolver resolver = RagnarIntegrationHandler.getResolverFor(entity);
        String level = "?";
        String name = entity.getName().getString();
        boolean showClazz = resolver != null;
        String clazz = "";
        String secondaryLabel = "";
        int labelColor = 0xFFFFFF;
        int secondaryLabelColor = 0xD0D0D0;
        String labelPrefix = "";
        int barFrameColor = 0xAA202020;
        int barAccentColor = 0;

        if (resolver != null) {
            String resolvedName = resolver.getDisplayName(entity);
            String lvl = resolver.getLevel(entity);
            String cls = showClazz ? resolver.getClazz(entity) : "";
            String secondary = resolver.getSecondaryLabel(entity);
            name = safeText(resolvedName, name);
            if (lvl != null && !lvl.isEmpty())
                level = lvl;
            if (cls != null && !cls.isEmpty())
                clazz = cls;
            if (secondary != null && !secondary.isEmpty())
                secondaryLabel = secondary;
            labelColor = resolver.getPrimaryLabelColor(entity);
            secondaryLabelColor = resolver.getSecondaryLabelColor(entity);
            labelPrefix = safeText(resolver.getPrimaryLabelPrefix(entity), "");
            barFrameColor = resolver.getHealthBarFrameColor(entity);
            barAccentColor = resolver.getHealthBarAccentColor(entity);
        }

        if (level.isEmpty() || level.equals("0"))
            level = "?";

        // === Etiqueta unificada ===
        net.minecraft.world.entity.MobCategory cat = entity.getType().getCategory();
        boolean isPassive = cat == net.minecraft.world.entity.MobCategory.CREATURE
                || cat == net.minecraft.world.entity.MobCategory.AMBIENT
                || cat == net.minecraft.world.entity.MobCategory.WATER_CREATURE;

        String label;
        if (isPassive) {
            String passiveLevel = isMissingLevel(level) ? "1" : level;
            label = "Lv " + passiveLevel + " " + name;
        } else {
            label = labelPrefix + "Lv " + level + " " + name;
            if (showClazz && !clazz.isEmpty()) {
                label += " " + clazz;
            }
        }

        boolean showDetails = player.isShiftKeyDown();
        boolean hasSecondaryLabel = showDetails && secondaryLabel != null && !secondaryLabel.isEmpty();
        float mainLabelY = 0.0F;
        float secondaryLabelY = 6.0F;
        float barY = hasSecondaryLabel ? 16.0F : 10.0F;
        float hpLabelY = barY + BAR_HEIGHT + 2.0F;

        // === Render ===
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(0.0D, entity.getBbHeight() + 0.8D, 0.0D);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025F * SCALE, -0.025F * SCALE, 0.025F * SCALE);

        Font font = mc.font;

        // === Barra de HP si fue golpeado recientemente
        Long lastHit = lastHitTime.get(entity);
        boolean recentlyHit = lastHit != null && (System.currentTimeMillis() - lastHit < DISPLAY_TIME_MS);
        boolean representedByTargetFrame = isRepresentedByTargetFrame(mc, entity);

        if (recentlyHit && !representedByTargetFrame) {
            float hp = entity.getHealth();
            float max = entity.getMaxHealth();
            if (max > 0f) {
                float pct = Math.max(0f, Math.min(1f, hp / max));
                drawCompactBar(ps, barY, pct, barFrameColor, barAccentColor);

                if (MobConfigAccess.renderNumericHealth()) {
                    String hpText = String.format(java.util.Locale.ROOT, "%.0f / %.0f", hp, max);
                    ps.pushPose();
                    float textScale = 0.45f;
                    ps.translate(0, hpLabelY, 0);
                    ps.scale(textScale, textScale, textScale);

                    font.drawInBatch(hpText, -font.width(hpText) / 2f, 0, 0xFFFFFFFF, true,
                            ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
                    ps.popPose();
                }
            }
        }

        font.drawInBatch(label, -font.width(label) / 2f, mainLabelY, labelColor, false,
                ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());

        if (hasSecondaryLabel) {
            ps.pushPose();
            float secondaryScale = 0.6F;
            ps.translate(0.0D, secondaryLabelY, 0.0D);
            ps.scale(secondaryScale, secondaryScale, secondaryScale);
            font.drawInBatch(secondaryLabel, -font.width(secondaryLabel) / 2f, 0, secondaryLabelColor, false,
                    ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
            ps.popPose();
        }

        ps.popPose();
    }

    private static void drawCompactBar(PoseStack ps, float y, float pct, int frameColor, int accentColor) {
        int width = BAR_WIDTH;
        int height = BAR_HEIGHT;
        int x1 = -width / 2;
        int innerX1 = x1 + 1;
        int innerY = Math.round(y) + 1;
        int innerWidth = Math.max(0, width - 2);
        int innerHeight = Math.max(1, height - 2);
        int filled = Math.round(innerWidth * pct);

        Matrix4f mat = ps.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.getBuilder();

        int bg = 0x66000000;
        int fill = lerpHpColor(pct);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(mat, buffer, x1, y, x1 + width, y + height, frameColor);
        BufferUploader.drawWithShader(buffer.end());

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(mat, buffer, innerX1, innerY, innerX1 + innerWidth, innerY + innerHeight, bg);
        BufferUploader.drawWithShader(buffer.end());

        if (filled > 0) {
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(mat, buffer, innerX1, innerY, innerX1 + filled, innerY + innerHeight, fill);
            BufferUploader.drawWithShader(buffer.end());
        }

        if (accentColor != 0) {
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(mat, buffer, innerX1, innerY, innerX1 + innerWidth, innerY + 1, accentColor);
            BufferUploader.drawWithShader(buffer.end());
        }

        if (pct <= 0f) {
            // keep the bar visible even when no HP fill is drawn
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillRect(mat, buffer, innerX1, innerY, innerX1, innerY + innerHeight, fill);
            BufferUploader.drawWithShader(buffer.end());
        }

        RenderSystem.disableBlend();
    }

    private static boolean isMissingLevel(String level) {
        return level == null || level.isEmpty() || level.equals("?") || level.equals("0");
    }

    private static boolean isRepresentedByTargetFrame(Minecraft mc, LivingEntity entity) {
        if (mc == null || entity == null) {
            return false;
        }
        if (!RagnarConfigs.CLIENT.hud.enabled.get() || !RagnarConfigs.CLIENT.hud.targetFrame.enabled.get()) {
            return false;
        }
        return mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity().getId() == entity.getId();
    }

    private static String safeText(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static void fillRect(Matrix4f mat, BufferBuilder buffer, float x1, float y1, float x2, float y2, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        buffer.vertex(mat, x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x2, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
    }

    private static int lerpHpColor(float pct) {
        pct = Math.max(0f, Math.min(1f, pct));
        int r, g, b = 0;
        if (pct >= 0.5f) {
            float t = (pct - 0.5f) / 0.5f;
            r = (int) (255 * (1f - t));
            g = 255;
        } else {
            float t = pct / 0.5f;
            r = 255;
            g = (int) (255 * t);
        }
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
