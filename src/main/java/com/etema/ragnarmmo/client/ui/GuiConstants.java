package com.etema.ragnarmmo.client.ui;

/**
 * Centralized constants for vanilla-style GUI layouts.
 * Based on Minecraft's standard container dimensions.
 */
public final class GuiConstants {

    // === Vanilla Slot Dimensions ===
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_INNER = 16;

    // === Panel Padding ===
    public static final int PADDING = 8;
    public static final int TITLE_HEIGHT = 17;

    // === Skills Screen ===
    public static final int SKILLS_PANEL_WIDTH = 280;
    public static final int SKILLS_PANEL_HEIGHT = 200;
    public static final int SKILLS_GRID_COLS = 7;
    public static final int SKILLS_GRID_ROWS = 4;

    // === Stats Screen ===
    public static final int STATS_PANEL_WIDTH = 200;
    public static final int STATS_PANEL_HEIGHT = 240;

    // === Colors ===
    public static final int COLOR_PANEL_BG = 0xC0101010;
    public static final int COLOR_PANEL_BORDER = 0xFF404040;
    public static final int COLOR_TITLE = 0xFFFFAA00;
    public static final int COLOR_TEXT = 0xFFFFFFFF;
    public static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    public static final int COLOR_LOCKED = 0x80000000;
    public static final int COLOR_MAXED = 0xFFFFD700;
    public static final int COLOR_LEARNED = 0x2000FF00;

    // === HUD Bar Colors (gradient top / bottom) ===
    public static final int COLOR_HP_BAR = 0xFFE04040;
    public static final int COLOR_HP_BAR_DARK = 0xFF901818;
    public static final int COLOR_SP_BAR = 0xFF4060E0;
    public static final int COLOR_SP_BAR_DARK = 0xFF182890;
    public static final int COLOR_SP_BAR_PHYS = 0xFFE0C020;
    public static final int COLOR_SP_BAR_PHYS_DARK = 0xFF907010;
    public static final int COLOR_XP_BAR = 0xFF40C050;
    public static final int COLOR_XP_BAR_DARK = 0xFF188028;
    public static final int COLOR_JOB_XP_BAR = 0xFF4080D0;
    public static final int COLOR_JOB_XP_BAR_DARK = 0xFF184890;

    // === HUD Panel Colors ===
    public static final int COLOR_HUD_PANEL_BG = 0xD0151520;
    public static final int COLOR_HUD_PANEL_BORDER_OUTER = 0xFF202030;
    public static final int COLOR_HUD_PANEL_BORDER_INNER = 0xFF404060;
    public static final int COLOR_BAR_BG = 0xFF101018;
    public static final int COLOR_BAR_HIGHLIGHT = 0x30FFFFFF;

    private GuiConstants() {
    }
}
