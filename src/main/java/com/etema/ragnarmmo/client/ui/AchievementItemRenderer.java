package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.achievements.capability.IPlayerAchievements;
import com.etema.ragnarmmo.achievements.data.AchievementDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * Pure static renderer for a single achievement list row.
 * No state, no instance — called from AchievementScreen per visible item.
 *
 * Separating this logic keeps AchievementScreen focused on layout and input
 * while making it easy to iterate on the visual style independently.
 *
 * All APIs used here are verified real in Forge 1.20.1.
 */
public final class AchievementItemRenderer {

    // ── Row item color constants ───────────────────────────────────────────────
    private static final int COL_ITEM_LOCKED   = 0xFF111122;
    private static final int COL_ITEM_UNLOCKED = 0xFF1B3A2A;
    private static final int COL_ITEM_CLAIMED  = 0xFF163052;
    private static final int COL_ITEM_ACTIVE   = 0xFF2A1060;

    private static final int COL_TEXT_NAME      = 0xFFFFD700;
    private static final int COL_TEXT_LOCKED    = 0xFF556677;
    private static final int COL_TEXT_DESC      = 0xFFCCCCCC;
    private static final int COL_TEXT_DESC_LOCK = 0xFF445566;
    private static final int COL_TEXT_POINTS    = 0xFFFFAA00;
    private static final int COL_TEXT_TITLE     = 0xFF55FFFF;
    private static final int COL_TEXT_TITLE_ACT = 0xFFFFFFFF;
    private static final int COL_TEXT_PROGRESS  = 0xFF888888;
    private static final int COL_TEXT_CLAIM     = 0xFF55FF55;
    private static final int COL_TEXT_CLAIMED   = 0xFF5588FF;
    private static final int COL_TEXT_EQUIP     = 0xFF55FF55;
    private static final int COL_TEXT_EQUIPPED  = 0xFF55FFFF;

    // Row geometry
    static final int ITEM_HEIGHT = 44; // must match AchievementScreen.ITEM_HEIGHT

    private AchievementItemRenderer() { /* static use only */ }

    /**
     * Draws one achievement row.
     *
     * @param g         GuiGraphics context (Forge 1.20.1)
     * @param font      Font from the parent Screen
     * @param def       Achievement definition
     * @param cap       Player capability (read-only here)
     * @param x         Left edge of the row (after list padding)
     * @param y         Top edge of the row in screen coords
     * @param rowWidth  Width available for this row (excludes scrollbar space)
     * @param hovered   True if mouse is inside the row bounds
     */
    public static void render(GuiGraphics g, Font font,
                              AchievementDefinition def,
                              IPlayerAchievements cap,
                              int x, int y, int rowWidth,
                              boolean hovered) {

        boolean unlocked = cap.isUnlocked(def.id());
        boolean claimed  = cap.isClaimed(def.id());
        boolean hasTitle = def.title() != null && !def.title().isEmpty();
        boolean isActive = hasTitle && def.title().equals(cap.getActiveTitle());

        int h = ITEM_HEIGHT - 2; // 2-px gap between rows

        // ── Background ────────────────────────────────────────────────────────
        int bg = isActive ? COL_ITEM_ACTIVE
               : claimed  ? COL_ITEM_CLAIMED
               : unlocked ? COL_ITEM_UNLOCKED
                          : COL_ITEM_LOCKED;
        if (hovered) bg = blendColor(bg, 0xFFFFFFFF, 0.07f);
        g.fill(x, y, x + rowWidth, y + h, bg);

        // ── Left accent stripe (3 px wide) ────────────────────────────────────
        int stripe = isActive ? 0xFFAA55FF
                   : claimed  ? 0xFF5588FF
                   : unlocked ? 0xFF55FF55
                              : 0xFF334455;
        g.fill(x, y, x + 3, y + h, stripe);

        // ── Border ────────────────────────────────────────────────────────────
        int border = isActive ? 0xFF7733CC
                   : hovered  ? 0xFF667799
                              : 0xFF2A3A50;
        g.renderOutline(x, y, rowWidth, h, border);

        // ── Right-side status text (drawn first to reserve space) ─────────────
        String statusText  = resolveStatus(def, cap, unlocked, claimed, hasTitle, isActive);
        int    statusColor = resolveStatusColor(def, unlocked, claimed, hasTitle, isActive);
        int    statusX     = x + rowWidth - 4; // bottom-right anchor, right-aligned

        if (!statusText.isEmpty()) {
            int sw = font.width(statusText);
            g.drawString(font, statusText, statusX - sw, y + 4, statusColor, false);
        }

        // ── Points badge (below status, right-aligned) ────────────────────────
        int pointsBadgeX = statusX;
        if (def.points() > 0) {
            String pts = "+" + def.points();
            int pw = font.width(pts);
            g.drawString(font, pts, pointsBadgeX - pw, y + 14, COL_TEXT_POINTS, false);
        }

        // ── Name (left side, truncated to avoid status/points collision) ──────
        // Reserve: stripe (3) + inner pad (4) on left; status width + points badge + pad on right
        int statusReserve = statusText.isEmpty() ? 0 : font.width(statusText) + 6;
        int pointsReserve = def.points() > 0 ? font.width("+" + def.points()) + 6 : 0;
        int rightReserve  = Math.max(statusReserve, pointsReserve);
        int nameMaxW      = rowWidth - 7 - rightReserve; // 7 = 3 stripe + 4 inner pad

        int nameColor = unlocked ? COL_TEXT_NAME : COL_TEXT_LOCKED;
        String nameStr = Component.translatable(def.name()).getString();
        String truncatedName = font.plainSubstrByWidth(nameStr, nameMaxW);
        g.drawString(font, truncatedName, x + 7, y + 4, nameColor, false);

        // ── Description (second line, truncated) ──────────────────────────────
        // Description should not overlap with points badge on second line
        int descMaxW  = rowWidth - 7 - pointsReserve;
        int descColor = unlocked ? COL_TEXT_DESC : COL_TEXT_DESC_LOCK;
        String descStr = Component.translatable(def.description()).getString();
        String truncatedDesc = font.plainSubstrByWidth(descStr, descMaxW);
        g.drawString(font, truncatedDesc, x + 7, y + 14, descColor, false);

        // ── Title line (third line, only if achievement grants a title) ───────
        if (hasTitle) {
            String translatedTitle = Component.translatable(def.title()).getString();
            String prefix = isActive ? "★ " : "○ ";
            String titleRaw = prefix + "[" + translatedTitle + "]";
            // Truncate title too, leaving room for equip status on the right
            int equipReserve = 0;
            if (claimed) {
                String equipStatus = isActive
                        ? Component.translatable("gui.ragnarmmo.achievements.equipped").getString()
                        : Component.translatable("gui.ragnarmmo.achievements.equip").getString();
                equipReserve = font.width(equipStatus) + 6;
            }
            int titleMaxW = rowWidth - 7 - equipReserve;
            int tc = isActive ? COL_TEXT_TITLE_ACT : COL_TEXT_TITLE;
            String truncatedTitle = font.plainSubstrByWidth(titleRaw, titleMaxW);
            g.drawString(font, truncatedTitle, x + 7, y + 24, tc, false);
        }

        // ── Progress bar (bottom area, only for non-level_up locked achievements) ──
        if (!unlocked && !def.triggerType().equals("level_up")) {
            int progress = cap.getProgress(def.id() + "_progress");
            String progText = progress + " / " + def.requiredAmount();

            // Progress bar: fits between the left stripe and right edge
            int barW  = 55;
            int barX  = x + rowWidth - barW - 4;
            int barY  = y + h - 8;
            float pct = def.requiredAmount() > 0
                    ? Mth.clamp((float) progress / def.requiredAmount(), 0f, 1f) : 0f;

            g.fill(barX, barY, barX + barW, barY + 4, 0xFF0D1A0D);
            if (pct > 0f) {
                g.fill(barX, barY, barX + (int)(barW * pct), barY + 4, 0xFF33AA55);
            }
            g.renderOutline(barX, barY, barW, 4, 0xFF2A4A30);

            // Progress text — left of the bar
            int ptw = font.width(progText);
            g.drawString(font, progText, barX - ptw - 4, barY - 1, COL_TEXT_PROGRESS, false);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveStatus(AchievementDefinition def,
                                         IPlayerAchievements cap,
                                         boolean unlocked, boolean claimed,
                                         boolean hasTitle, boolean isActive) {
        if (unlocked && !claimed) {
            return Component.translatable("gui.ragnarmmo.achievements.claimable").getString();
        }
        if (claimed) {
            if (hasTitle) {
                return isActive
                        ? Component.translatable("gui.ragnarmmo.achievements.equipped").getString()
                        : Component.translatable("gui.ragnarmmo.achievements.equip").getString();
            }
            return Component.translatable("gui.ragnarmmo.achievements.claimed").getString();
        }
        return Component.translatable("gui.ragnarmmo.achievements.locked").getString();
    }

    private static int resolveStatusColor(AchievementDefinition def,
                                          boolean unlocked, boolean claimed,
                                          boolean hasTitle, boolean isActive) {
        if (unlocked && !claimed) return COL_TEXT_CLAIM;
        if (claimed) {
            if (hasTitle) return isActive ? COL_TEXT_EQUIPPED : COL_TEXT_EQUIP;
            return COL_TEXT_CLAIMED;
        }
        return COL_TEXT_LOCKED;
    }

    /**
     * Linearly blends two ARGB colors.
     * @param t 0.0 = 100% base, 1.0 = 100% overlay
     */
    private static int blendColor(int base, int overlay, float t) {
        int aB = (base    >> 24) & 0xFF, rB = (base    >> 16) & 0xFF,
            gB = (base    >>  8) & 0xFF, bB =  base           & 0xFF;
        int aO = (overlay >> 24) & 0xFF, rO = (overlay >> 16) & 0xFF,
            gO = (overlay >>  8) & 0xFF, bO =  overlay         & 0xFF;
        return ((int)(aB + (aO - aB) * t) << 24)
             | ((int)(rB + (rO - rB) * t) << 16)
             | ((int)(gB + (gO - gB) * t) <<  8)
             |  (int)(bB + (bO - bB) * t);
    }
}
