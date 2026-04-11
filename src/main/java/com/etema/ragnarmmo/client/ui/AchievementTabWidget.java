package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Custom tab widget that renders completely via GuiGraphics primitives,
 * bypassing the vanilla Button grey-texture look.
 *
 * Compatible with Forge 1.20.1 / net.minecraft.client.gui.components.AbstractWidget.
 */
public class AchievementTabWidget extends AbstractWidget {

    // ── Visual palette (matches AchievementScreen constants) ──────────────────
    private static final int COL_ACTIVE      = 0xFF533483;
    private static final int COL_INACTIVE    = 0xFF0F3460;
    private static final int COL_HOVER       = 0xFF3D2760;
    private static final int COL_ACCENT_LINE = 0xFFFFD700;
    private static final int COL_TEXT_ON     = 0xFFFFFFFF;
    private static final int COL_TEXT_OFF    = 0xFFAABBCC;
    private static final int COL_BORDER_ACT  = 0xFF7744BB;
    private static final int COL_BORDER_INACT= 0xFF1A3A5C;

    private boolean active;
    private final Runnable onPress;

    /**
     * @param x       left edge in GUI coords
     * @param y       top edge in GUI coords
     * @param width   width in GUI coords
     * @param height  height in GUI coords
     * @param label   display text (usually a translatable Component)
     * @param active  whether this tab is currently selected
     * @param onPress callback fired on left-click
     */
    public AchievementTabWidget(int x, int y, int width, int height,
                                Component label, boolean active, Runnable onPress) {
        super(x, y, width, height, label);
        this.active  = active;
        this.onPress = onPress;
    }

    /** Updates active state without rebuilding the widget. */
    public void setActive(boolean active) {
        this.active = active;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        // Background: active → accent purple, hover → slightly lighter, inactive → dark blue
        int bg;
        if (active) {
            bg = COL_ACTIVE;
        } else if (isHovered()) {
            bg = COL_HOVER;
        } else {
            bg = COL_INACTIVE;
        }
        g.fill(x, y, x + w, y + h, bg);

        // 1-px outer border
        int border = active ? COL_BORDER_ACT : COL_BORDER_INACT;
        g.renderOutline(x, y, w, h, border);

        // Bottom accent line on active tab (2 px thick, gold)
        if (active) {
            g.fill(x + 1, y + h - 2, x + w - 1, y + h, COL_ACCENT_LINE);
        }

        // Top highlight line (subtle light edge) on hover / active
        if (active || isHovered()) {
            g.fill(x + 1, y, x + w - 1, y + 1, 0x44FFFFFF);
        }

        // Label — centered, truncated if too wide
        int textColor = active ? COL_TEXT_ON : COL_TEXT_OFF;
        // AbstractWidget does not expose 'font'; use Minecraft.getInstance().font (safe on client thread)
        net.minecraft.client.gui.Font mcFont = Minecraft.getInstance().font;
        // Reserve 4 px margin on each side before truncating
        int availW = w - 8;
        // Use String based truncation and rendering to avoid FormattedCharSequence overload issues
        String labelStr = this.getMessage().getString();
        String truncated = mcFont.plainSubstrByWidth(labelStr, availW);
        int textW = mcFont.width(truncated);
        int tx = x + (w - textW) / 2;
        int ty = y + (h - 8) / 2; // 8 = font pixel height in Minecraft
        g.drawString(mcFont, truncated, tx, ty, textColor, false);
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onPress != null) onPress.run();
    }

    // ── Narration (accessibility) ──────────────────────────────────────────────

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Delegate to the default name narration supplied by AbstractWidget
        this.defaultButtonNarrationText(output);
    }
}
