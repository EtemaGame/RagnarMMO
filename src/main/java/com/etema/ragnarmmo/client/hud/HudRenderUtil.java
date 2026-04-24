package com.etema.ragnarmmo.client.hud;

import com.etema.ragnarmmo.client.ui.GuiConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * MMO Classic (Ragnarok) style rendering helpers for the HUD.
 * Uses gradient bars, double-bordered panels, and simulated rounded corners.
 */
public final class HudRenderUtil {

    private HudRenderUtil() {
    }

    // ── Panels ────────────────────────────────────────────────────────

    /**
     * Draws a semi-transparent panel with double border (outer dark, inner lighter).
     * Simulates rounded corners by clearing corner pixels.
     */
    public static void drawPanel(GuiGraphics gg, int x, int y, int w, int h) {
        // Fill background
        gg.fill(x, y, x + w, y + h, GuiConstants.COLOR_HUD_PANEL_BG);

        // Outer border (dark)
        int o = GuiConstants.COLOR_HUD_PANEL_BORDER_OUTER;
        gg.fill(x, y, x + w, y + 1, o);           // top
        gg.fill(x, y + h - 1, x + w, y + h, o);   // bottom
        gg.fill(x, y, x + 1, y + h, o);            // left
        gg.fill(x + w - 1, y, x + w, y + h, o);    // right

        // Inner border (lighter, 1px inset)
        int i = GuiConstants.COLOR_HUD_PANEL_BORDER_INNER;
        gg.fill(x + 1, y + 1, x + w - 1, y + 2, i);           // top
        gg.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, i);   // bottom
        gg.fill(x + 1, y + 1, x + 2, y + h - 1, i);            // left
        gg.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, i);    // right

        // Simulated rounded corners - clear the 4 outer corner pixels
        int clear = 0x00000000;
        gg.fill(x, y, x + 1, y + 1, clear);                     // top-left
        gg.fill(x + w - 1, y, x + w, y + 1, clear);             // top-right
        gg.fill(x, y + h - 1, x + 1, y + h, clear);             // bottom-left
        gg.fill(x + w - 1, y + h - 1, x + w, y + h, clear);     // bottom-right
    }

    // ── Bars ──────────────────────────────────────────────────────────

    /**
     * Draws a gradient bar with dark background, fill gradient, and glass highlight.
     *
     * @param colorTop    Top color of the gradient fill
     * @param colorBottom Bottom color of the gradient fill
     */
    public static void drawGradientBar(GuiGraphics gg, int x, int y, int w, int h,
            float progress, int colorTop, int colorBottom) {
        progress = Math.max(0f, Math.min(1f, progress));
        int fill = Math.round(progress * (w - 2)); // -2 for 1px border on each side

        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = w - 2;
        int innerH = h - 2;

        // Dark background for the entire bar area
        gg.fill(x, y, x + w, y + h, GuiConstants.COLOR_BAR_BG);

        // 1px border
        int border = 0xFF303040;
        gg.fill(x, y, x + w, y + 1, border);           // top
        gg.fill(x, y + h - 1, x + w, y + h, border);   // bottom
        gg.fill(x, y, x + 1, y + h, border);            // left
        gg.fill(x + w - 1, y, x + w, y + h, border);    // right

        // Gradient fill
        if (fill > 0) {
            gg.fillGradient(innerX, innerY, innerX + fill, innerY + innerH,
                    colorTop, colorBottom);

            // Glass highlight on top row (1px)
            if (innerH > 2) {
                gg.fill(innerX, innerY, innerX + fill, innerY + 1,
                        GuiConstants.COLOR_BAR_HIGHLIGHT);
            }
        }
    }

    /**
     * Draws a thin XP-style gradient bar (no border, just fill on dark bg).
     */
    public static void drawThinBar(GuiGraphics gg, int x, int y, int w, int h,
            float progress, int colorTop, int colorBottom) {
        progress = Math.max(0f, Math.min(1f, progress));
        int fill = Math.round(progress * w);

        // Dark background
        gg.fill(x, y, x + w, y + h, GuiConstants.COLOR_BAR_BG);

        // Gradient fill
        if (fill > 0) {
            gg.fillGradient(x, y, x + fill, y + h, colorTop, colorBottom);
        }
    }

    /**
     * Alternate compact bar renderer for the scattered layout.
     */
    public static void drawBarVanilla(GuiGraphics gg, int x, int y, int w, int h,
            float progress, int fillColor) {
        progress = Math.max(0f, Math.min(1f, progress));
        int fill = Math.round(progress * w);
        gg.fill(x, y, x + w, y + h, 0x66000000);
        if (fill > 0) {
            gg.fill(x, y, x + fill, y + h, fillColor);
        }
        int o = 0xAA000000;
        gg.fill(x, y, x + w, y + 1, o);
        gg.fill(x, y + h - 1, x + w, y + h, o);
        gg.fill(x, y, x + 1, y + h, o);
        gg.fill(x + w - 1, y, x + w, y + h, o);
    }

    // ── Text ──────────────────────────────────────────────────────────

    /**
     * Draws text with shadow for readability.
     */
    public static void drawTextShadow(GuiGraphics gg, Font font, String text, int x, int y, int color) {
        gg.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws right-aligned text with shadow.
     */
    public static void drawTextShadowRight(GuiGraphics gg, Font font, String text, int rightX, int y, int color) {
        int w = font.width(text);
        gg.drawString(font, text, rightX - w, y, color, true);
    }

    // ── Icons (Unicode placeholders - kept for SCATTERED mode) ────────

    public static void drawIconHeart(GuiGraphics gg, Font font, int x, int y) {
        gg.drawString(font, "\u2764", x, y, 0xFFFF5555, true);
    }

    public static void drawIconResource(GuiGraphics gg, Font font, int x, int y, int color) {
        gg.drawString(font, "\u2726", x, y, color, true);
    }

    public static void drawIconFood(GuiGraphics gg, Font font, int x, int y) {
        gg.drawString(font, "\u2615", x, y, 0xFFFFAA00, true);
    }

    public static void drawIconAir(GuiGraphics gg, Font font, int x, int y) {
        gg.drawString(font, "\u25CB", x, y, 0xFF55AAFF, true);
    }

    // ── Skill Slots ───────────────────────────────────────────────────

    public static void drawCooldownOverlay(GuiGraphics gg, int x, int y, int size, float progress) {
        if (progress <= 0f)
            return;
        int h = Math.round(size * Math.min(1f, progress));
        gg.fill(x, y, x + size, y + h, 0x88000000);
    }

    public static void drawGhostSlot(GuiGraphics gg, int x, int y, int w, int h) {
        int c = 0x22FFFFFF;
        gg.fill(x, y, x + w, y + 1, c);
        gg.fill(x, y + h - 1, x + w, y + h, c);
        gg.fill(x, y, x + 1, y + h, c);
        gg.fill(x + w - 1, y, x + w, y + h, c);
    }
}
