package com.etema.ragnarmmo.client.render;

import com.etema.ragnarmmo.system.bar.EntityStatResolver;
import com.etema.ragnarmmo.system.bar.RagnarIntegrationHandler;
import com.etema.ragnarmmo.system.mobstats.integration.MobInfoIntegration;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.WeakHashMap;

/**
 * Renderiza una barra de vida y etiquetas sobre mobs o jugadores
 * con integración automática a RagnarStats y RagnarMobStats.
 */
@Mod.EventBusSubscriber(modid = com.etema.ragnarmmo.RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RagnarBarRenderHandler {

    private static final double MAX_DISTANCE = 40.0D;
    private static final float SCALE = 1.0F;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 5;
    private static final long DISPLAY_TIME_MS = 3000;

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

        // === Datos base ===
        EntityStatResolver resolver = RagnarIntegrationHandler.getResolverFor(entity);
        String level = "?";
        String name = entity.getName().getString();
        boolean showClazz = resolver != null;
        String clazz = "";

        if (resolver != null) {
            String lvl = resolver.getLevel(entity);
            String cls = showClazz ? resolver.getClazz(entity) : "";
            if (lvl != null && !lvl.isEmpty())
                level = lvl;
            if (cls != null && !cls.isEmpty())
                clazz = cls;
        }

        boolean missingLevel = level == null || level.isEmpty() || level.equals("?") || level.equals("0");
        boolean missingClazz = clazz == null || clazz.isEmpty();
        if ((missingLevel || missingClazz)
                && entity.level().isClientSide()
                && RagnarIntegrationHandler.hasRagnarMobStats
                && !(entity instanceof Player)) {
            var infoOpt = MobInfoIntegration.getMobInfo(entity);
            if (missingLevel) {
                int resolvedLevel = infoOpt
                        .map(MobInfoIntegration.MobInfo::level)
                        .filter(lvl -> lvl > 0)
                        .orElseGet(() -> readPersistentMobLevel(entity).orElse(0));
                if (resolvedLevel > 0) {
                    level = String.valueOf(resolvedLevel);
                }
            }
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
            String passiveLevel = (level == null || level.isEmpty() || level.equals("?") || level.equals("0")) ? "1" : level;
            label = "Lv " + passiveLevel + " " + name;
        } else {
            String rank = resolver != null ? resolver.getRank(entity) : "";
            if (rank.isEmpty() && entity.level().isClientSide()) {
                rank = MobInfoIntegration.getMobTier(entity).map(t -> t.name()).orElse("");
            }

            if ("MINI_BOSS".equalsIgnoreCase(rank)) rank = "ELITE";
            else if ("MVP".equalsIgnoreCase(rank)) rank = "BOSS";

            String icon = "";
            if ("ELITE".equalsIgnoreCase(rank)) icon = "★ ";
            else if ("BOSS".equalsIgnoreCase(rank)) icon = "☠ ";

            label = icon + "Lv " + level + " " + name;
            if (showClazz && !clazz.isEmpty()) {
                label += " " + clazz;
            }
        }

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

        if (recentlyHit) {
            float hp = entity.getHealth();
            float max = entity.getMaxHealth();
            if (max > 0f) {
                float pct = Math.max(0f, Math.min(1f, hp / max));
                drawCompactBar(ps, 12, pct); // Bar moved down to make room for name in center

                if (MobConfig.RENDER_NUMERIC_HEALTH.get()) {
                    String hpText = String.format(java.util.Locale.ROOT, "%.0f / %.0f", hp, max);
                    ps.pushPose();
                    float textScale = 0.5f;
                    ps.translate(0, -12, 0); // Position ABOVE label
                    ps.scale(textScale, textScale, textScale);

                    font.drawInBatch(hpText, -font.width(hpText) / 2f, 0, 0xFFFFFFFF, true,
                            ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
                    ps.popPose();
                }
            }
        }

        // Draw label (Level and Name) at y=0 (Center)
        font.drawInBatch(label, -font.width(label) / 2f, 0, 0xFFFFFF, false,
                ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());

        ps.popPose();
    }

    private static void drawCompactBar(PoseStack ps, float y, float pct) {
        int width = BAR_WIDTH;
        int height = BAR_HEIGHT;
        int x1 = -width / 2;
        int filled = Math.round(width * pct);

        Matrix4f mat = ps.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.getBuilder();

        int bg = 0x66000000;
        int fill = lerpHpColor(pct);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(mat, buffer, x1, y, x1 + width, y + height, bg);
        BufferUploader.drawWithShader(buffer.end());

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        fillRect(mat, buffer, x1, y, x1 + filled, y + height, fill);
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableBlend();
    }

    private static OptionalInt readPersistentMobLevel(LivingEntity entity) {
        return getMobStatsTag(entity)
                .filter(tag -> tag.contains("Level", Tag.TAG_INT))
                .map(tag -> OptionalInt.of(Math.max(0, tag.getInt("Level"))))
                .orElse(OptionalInt.empty());
    }

    private static Optional<CompoundTag> getMobStatsTag(LivingEntity entity) {
        if (entity == null) return Optional.empty();
        CompoundTag data = entity.getPersistentData();
        if (data == null || !data.contains("RagnarMobStats", Tag.TAG_COMPOUND)) return Optional.empty();
        return Optional.of(data.getCompound("RagnarMobStats"));
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
