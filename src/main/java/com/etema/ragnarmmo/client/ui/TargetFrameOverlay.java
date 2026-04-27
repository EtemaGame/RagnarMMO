package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
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

    private static final int WIDTH = 118;
    private static final int BAR_HEIGHT = 4;
    private static final int LABEL_GAP = 2;
    private static final int PADDING = 4;

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
        float progress = maxHealth <= 0.0F ? 0.0F : Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        String hpText = Math.max(0, Math.round(health)) + "/" + Math.max(1, Math.round(maxHealth));
        int hpWidth = font.width(hpText);
        int nameWidth = WIDTH - PADDING * 2 - hpWidth - 6;
        String nameText = trimToWidth(font, name.getString(), nameWidth);

        graphics.fill(0, 0, WIDTH, getHeight(font), 0x76050608);
        graphics.fill(0, getHeight(font) - 1, WIDTH, getHeight(font), 0x99000000);
        graphics.drawString(font, nameText, PADDING, PADDING, 0xFFE8EDF5, true);
        graphics.drawString(font, hpText, WIDTH - PADDING - hpWidth, PADDING, 0xFFD7DEE8, true);

        int barY = PADDING + font.lineHeight + LABEL_GAP;
        int barX = PADDING;
        int barW = WIDTH - PADDING * 2;
        graphics.fill(barX, barY, barX + barW, barY + BAR_HEIGHT, 0xCC111216);
        graphics.fill(barX, barY, barX + Math.round(barW * progress), barY + BAR_HEIGHT, hpColor(progress));
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private static int hpColor(float progress) {
        if (progress > 0.55F) {
            return 0xFF68D383;
        }
        if (progress > 0.25F) {
            return 0xFFE6C55C;
        }
        return 0xFFE05C5C;
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
