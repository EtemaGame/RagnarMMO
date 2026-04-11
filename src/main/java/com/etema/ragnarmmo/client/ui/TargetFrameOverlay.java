package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
import com.etema.ragnarmmo.client.hud.HudRenderUtil;
import com.etema.ragnarmmo.client.hud.HudWidgetState;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TargetFrameOverlay implements IGuiOverlay {
    public static final TargetFrameOverlay INSTANCE = new TargetFrameOverlay();

    private static final int WIDTH = 140;
    private static final int BAR_HEIGHT = 8;
    private static final int LABEL_GAP = 3;
    private static final int PADDING = 3;

    private TargetFrameOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get() || !RagnarConfigs.CLIENT.hud.targetFrame.enabled.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }

        LivingEntity target = getCrosshairTarget(mc);
        if (target == null) {
            return;
        }

        Font font = mc.font;
        int height = getHeight(font);
        HudWidgetState state = HudConfigSerializer.read(RagnarConfigs.CLIENT.hud.targetFrame);
        HudLayoutManager.HudBounds bounds = HudLayoutManager.bounds(state, WIDTH, height, screenWidth, screenHeight);

        RenderSystem.enableBlend();
        HudLayoutManager.renderBackground(graphics, state, bounds);
        HudLayoutManager.pushWidgetTransform(graphics, bounds);
        renderTarget(graphics, font, target.getDisplayName(), target.getHealth(), target.getMaxHealth());
        HudLayoutManager.popWidgetTransform(graphics);
        RenderSystem.disableBlend();
    }

    public static int getWidth() {
        return WIDTH;
    }

    public static int getHeight(Font font) {
        return PADDING * 2 + font.lineHeight + LABEL_GAP + BAR_HEIGHT;
    }

    public static int renderPreview(GuiGraphics graphics, Font font) {
        renderTarget(graphics, font, Component.translatable("screen.ragnarmmo.overlay.target.preview"), 36.0F, 50.0F);
        return getHeight(font);
    }

    private static void renderTarget(GuiGraphics graphics, Font font, Component name, float health, float maxHealth) {
        String nameText = font.plainSubstrByWidth(name.getString(), WIDTH - PADDING * 2);
        graphics.drawString(font, nameText, PADDING, PADDING, 0xFFFFFFFF, true);

        float progress = maxHealth <= 0.0F ? 0.0F : Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        int barY = PADDING + font.lineHeight + LABEL_GAP;
        HudRenderUtil.drawGradientBar(graphics, PADDING, barY, WIDTH - PADDING * 2, BAR_HEIGHT, progress,
                0xFFFF7777, 0xFFAA2222);

        String hpText = Math.max(0, Math.round(health)) + "/" + Math.max(1, Math.round(maxHealth));
        int textWidth = font.width(hpText);
        graphics.drawString(font, hpText, WIDTH - PADDING - textWidth, barY, 0xFFFFFFFF, true);
    }

    private static LivingEntity getCrosshairTarget(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && living.isAlive()
                && living != mc.player) {
            return living;
        }
        return null;
    }
}
