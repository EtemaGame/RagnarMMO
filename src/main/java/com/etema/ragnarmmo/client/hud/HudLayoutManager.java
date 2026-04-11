package com.etema.ragnarmmo.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Shared positioning and transform helpers for HUD widgets.
 */
public final class HudLayoutManager {

    private HudLayoutManager() {
    }

    public static HudBounds bounds(HudWidgetState state, int width, int height, int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        double scale = Mth.clamp(state.scale(), 0.1D, 5.0D);
        int realWidth = Math.max(1, (int) Math.ceil(safeWidth * scale));
        int realHeight = Math.max(1, (int) Math.ceil(safeHeight * scale));

        int maxX = Math.max(0, screenWidth - realWidth);
        int maxY = Math.max(0, screenHeight - realHeight);
        int x = (int) Math.round(Mth.clamp(state.anchorX(), 0.0D, 1.0D) * maxX);
        int y = (int) Math.round(Mth.clamp(state.anchorY(), 0.0D, 1.0D) * maxY);
        return new HudBounds(x, y, safeWidth, safeHeight, realWidth, realHeight, scale);
    }

    public static Anchor anchorFromPixel(
            int pixelX,
            int pixelY,
            HudWidgetState state,
            int width,
            int height,
            int screenWidth,
            int screenHeight) {
        HudBounds bounds = bounds(state, width, height, screenWidth, screenHeight);
        int maxX = Math.max(0, screenWidth - bounds.realWidth());
        int maxY = Math.max(0, screenHeight - bounds.realHeight());
        int clampedX = Mth.clamp(pixelX, 0, maxX);
        int clampedY = Mth.clamp(pixelY, 0, maxY);
        double anchorX = maxX == 0 ? 0.0D : (double) clampedX / (double) maxX;
        double anchorY = maxY == 0 ? 0.0D : (double) clampedY / (double) maxY;
        return new Anchor(anchorX, anchorY);
    }

    public static void renderBackground(GuiGraphics graphics, HudWidgetState state, HudBounds bounds) {
        if (!state.showBackground() || state.backgroundAlpha() <= 0) {
            return;
        }
        int alpha = Mth.clamp(state.backgroundAlpha(), 0, 255);
        int bgColor = (alpha << 24) | 0x000000;
        graphics.fill(
                bounds.x() - 2,
                bounds.y() - 2,
                bounds.x() + bounds.realWidth() + 2,
                bounds.y() + bounds.realHeight() + 2,
                bgColor);
    }

    public static void pushWidgetTransform(GuiGraphics graphics, HudBounds bounds) {
        graphics.pose().pushPose();
        graphics.pose().translate(bounds.x(), bounds.y(), 0);
        graphics.pose().scale((float) bounds.scale(), (float) bounds.scale(), 1.0F);
    }

    public static void popWidgetTransform(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    public record HudBounds(
            int x,
            int y,
            int width,
            int height,
            int realWidth,
            int realHeight,
            double scale) {
    }

    public record Anchor(double x, double y) {
    }
}
