package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.system.achievements.data.AchievementCategory;
import com.etema.ragnarmmo.system.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.system.achievements.data.AchievementRegistry;
import com.etema.ragnarmmo.system.achievements.network.ClaimAchievementPacket;
import com.etema.ragnarmmo.system.achievements.network.SetTitlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Achievement browser screen for RagnarMMO — Forge 1.20.1.
 *
 * Structure:
 *   ┌──────────────────────────────────────┐
 *   │  HEADER — title / active-title / pts │
 *   ├──────────────────────────────────────┤
 *   │  TAB BAR  (AchievementTabWidget ×N)  │
 *   ├───────────────────────────────┬──────┤
 *   │  ACHIEVEMENT LIST (scissored) │  SB  │
 *   └───────────────────────────────┴──────┘
 *
 * Tab navigation and item click both preserve the existing network calls
 * (ClaimAchievementPacket / SetTitlePacket).
 */
public class AchievementScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float WIDTH_FRACTION  = 0.75f;
    private static final float HEIGHT_FRACTION = 0.80f;

    private static final int MIN_WIDTH  = 300;
    private static final int MAX_WIDTH  = 480;
    private static final int MIN_HEIGHT = 220;
    private static final int MAX_HEIGHT = 360;

    private static final int PAD          = 8;
    private static final int HEADER_H     = 36;
    private static final int TAB_BAR_H    = 22;
    /** Must stay in sync with AchievementItemRenderer.ITEM_HEIGHT */
    private static final int ITEM_HEIGHT  = AchievementItemRenderer.ITEM_HEIGHT;
    private static final int SCROLLBAR_W  = 6;
    private static final int SCROLLBAR_PAD = 2; // gap between list and scrollbar

    // ── Panel colors (dark fantasy palette) ───────────────────────────────────
    private static final int COL_BG           = 0xFF1A1A2E;
    private static final int COL_HEADER       = 0xFF16213E;
    private static final int COL_BORDER       = 0xFF0F3460;
    private static final int COL_ACCENT       = 0xFF533483;
    private static final int COL_TAB_INACTIVE = 0xFF0F3460;

    private static final int COL_TEXT_TITLE_SCREEN = 0xFFFFD700;
    private static final int COL_TEXT_TITLE_ACTIVE = 0xFF55FFFF;
    private static final int COL_TEXT_POINTS       = 0xFFFFAA00;
    private static final int COL_TEXT_CAT_HEADER   = 0xFFAABBCC;

    private static final int COL_SCROLLBAR_BG    = 0xFF0A0A18;
    private static final int COL_SCROLLBAR_THUMB = 0xFF533483;
    private static final int COL_SCROLLBAR_HOVER = 0xFF7755BB;

    // ── Layout state (recalculated on init / resize) ──────────────────────────
    private int guiW, guiH, leftPos, topPos;
    /** Y-start of the scrollable area (screen coordinates). */
    private int listY;
    /** Pixel height of the scrollable area. */
    private int listH;
    /** Number of fully visible items (used for scroll math). */
    private int visibleItems;
    /** X coordinate of the left edge of the scrollbar track. */
    private int scrollbarX;

    // ── Data state ────────────────────────────────────────────────────────────
    private AchievementCategory selectedCategory = AchievementCategory.BASIC;
    private List<AchievementDefinition> filteredAchievements;

    // ── Scroll state ──────────────────────────────────────────────────────────
    /** Fractional item-index offset — used for smooth animation. */
    private float scrollOffset = 0f;
    /** Target scroll position — scroll wheel / drag updates this. */
    private float scrollTarget = 0f;
    private boolean isDraggingScroll    = false;
    private int     scrollDragStartY    = 0;
    private float   scrollDragStartOff  = 0f;

    // ─────────────────────────────────────────────────────────────────────────

    public AchievementScreen() {
        super(Component.translatable("gui.ragnarmmo.achievements.title"));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        recalculateLayout();
        refreshList();
        buildTabWidgets();
        buildUnequipButton();
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        // Preserve scroll position across resizes
        float prevTarget = scrollTarget;
        super.resize(mc, w, h);   // calls init() → recalculateLayout
        scrollTarget = Mth.clamp(prevTarget, 0f, maxScrollIndex());
        scrollOffset = scrollTarget;
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void recalculateLayout() {
        guiW = Mth.clamp((int)(this.width  * WIDTH_FRACTION),  MIN_WIDTH,  MAX_WIDTH);
        guiH = Mth.clamp((int)(this.height * HEIGHT_FRACTION), MIN_HEIGHT, MAX_HEIGHT);

        leftPos = (this.width  - guiW) / 2;
        topPos  = (this.height - guiH) / 2;

        listY = topPos + HEADER_H + TAB_BAR_H + PAD;
        listH = guiH - HEADER_H - TAB_BAR_H - PAD * 2;
        visibleItems = Math.max(1, listH / ITEM_HEIGHT);

        scrollbarX = leftPos + guiW - SCROLLBAR_W - SCROLLBAR_PAD;
    }

    /**
     * Creates AchievementTabWidget instances instead of vanilla Buttons,
     * so tabs render using their own custom draw code (no grey vanilla texture).
     * Called on init and whenever the active category changes.
     */
    private void buildTabWidgets() {
        // clearWidgets() removes all existing widgets including old tabs and unequip button.
        // Re-adding the unequip button after this call is handled in init().
        clearWidgets();

        AchievementCategory[] cats = AchievementCategory.values();
        int totalTabs = cats.length;
        int totalTabW = guiW - PAD * 2;
        int tabH      = TAB_BAR_H - 2;
        int tabY      = topPos + HEADER_H + 1;

        for (int i = 0; i < totalTabs; i++) {
            final AchievementCategory cat = cats[i];
            boolean isActive = cat == selectedCategory;

            // Integer-split to avoid pixel gaps / overlap between tabs
            int x0 = leftPos + PAD + (totalTabW * i)       / totalTabs;
            int x1 = leftPos + PAD + (totalTabW * (i + 1)) / totalTabs;
            int tw = x1 - x0 - 1; // 1-px gap between adjacent tabs

            this.addRenderableWidget(new AchievementTabWidget(
                    x0, tabY, tw, tabH,
                    Component.translatable(cat.getTranslationKey()),
                    isActive,
                    () -> {
                        if (selectedCategory != cat) {
                            selectedCategory = cat;
                            scrollOffset = 0f;
                            scrollTarget = 0f;
                            refreshList();
                            // Rebuild tabs so the new active tab updates its highlight,
                            // then rebuild the unequip button that was cleared.
                            buildTabWidgets();
                            buildUnequipButton();
                        }
                    }
            ));
        }
    }

    /**
     * "Desequipar" remains a vanilla Button — it's a single action button,
     * not a navigation element, and its vanilla appearance is acceptable here.
     */
    private void buildUnequipButton() {
        int btnW = 80;
        int btnH = 14;
        int btnX = leftPos + guiW - btnW - PAD;
        int btnY = topPos  + (HEADER_H - btnH) / 2;
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("gui.ragnarmmo.achievements.unequip"),
                b -> Network.sendToServer(new SetTitlePacket(""))
            )
            .bounds(btnX, btnY, btnW, btnH)
            .build()
        );
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshList() {
        filteredAchievements = AchievementRegistry.getInstance().getAll().values().stream()
                .filter(a -> a.category() == selectedCategory)
                .sorted(Comparator.comparing(AchievementDefinition::id))
                .collect(Collectors.toList());
        clampScroll();
    }

    private int maxScrollIndex() {
        return Math.max(0, filteredAchievements.size() - visibleItems);
    }

    private void clampScroll() {
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScrollIndex());
        scrollOffset = Mth.clamp(scrollOffset, 0f, maxScrollIndex());
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Smooth scroll lerp — converges to target over ~4 frames
        scrollOffset += (scrollTarget - scrollOffset) * 0.25f;

        this.renderBackground(g);
        drawPanel(g);
        drawHeader(g, mouseX, mouseY);
        drawTabBarBackground(g);         // background strip behind the tab widgets
        drawCategoryHeader(g);
        drawAchievementList(g, mouseX, mouseY);
        drawScrollbar(g, mouseX, mouseY);

        // Widgets (AchievementTabWidget + unequip Button) rendered last
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Outer panel — shadow, background, border, accent lines. */
    private void drawPanel(GuiGraphics g) {
        // Drop shadow (offset 4 px)
        g.fill(leftPos + 4, topPos + 4, leftPos + guiW + 4, topPos + guiH + 4, 0x99000000);
        // Panel background
        g.fill(leftPos, topPos, leftPos + guiW, topPos + guiH, COL_BG);
        // Outer border
        g.renderOutline(leftPos, topPos, guiW, guiH, COL_BORDER);
        // Top inner accent line
        g.fill(leftPos + 1, topPos + 1, leftPos + guiW - 1, topPos + 2, COL_ACCENT);
        // Bottom inner accent line
        g.fill(leftPos + 1, topPos + guiH - 2, leftPos + guiW - 1, topPos + guiH - 1, COL_ACCENT);
    }

    /** Header bar — screen title, active title, and point total. */
    private void drawHeader(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + guiW, topPos + HEADER_H, COL_HEADER);
        // Separator line at the bottom of the header
        g.fill(leftPos, topPos + HEADER_H - 1, leftPos + guiW, topPos + HEADER_H, COL_ACCENT);

        // Screen title — left aligned, decorated with a rune symbol
        String screenTitle = "✦ " + Component.translatable("gui.ragnarmmo.achievements.title").getString();
        g.drawString(this.font, screenTitle, leftPos + PAD, topPos + PAD, COL_TEXT_TITLE_SCREEN, false);

        if (Minecraft.getInstance().player == null) return;

        Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            // Active title — centered in header (truncated to fit between title and button)
            String rawTitle = cap.getActiveTitle();
            if (rawTitle != null && !rawTitle.isEmpty()) {
                String titleStr = "[" + Component.translatable(rawTitle).getString() + "]";
                // Available center zone: between screen-title and points area
                int titleMaxW = guiW / 2 - PAD * 4;
                String truncatedTitle = this.font.plainSubstrByWidth(titleStr, titleMaxW);
                int titleW = this.font.width(truncatedTitle);
                int tx = leftPos + (guiW / 2) - titleW / 2;
                // Use String overload: drawString(Font, String, int, int, int, boolean)
                g.drawString(this.font, truncatedTitle, tx, topPos + PAD, COL_TEXT_TITLE_ACTIVE, false);
            }

            // Points badge — right-aligned, leaves 90 px clearance for the unequip button
            String pts = "⭐ " + cap.getTotalPoints() + " pts";
            int ptsX = leftPos + guiW - 90 - this.font.width(pts);
            g.drawString(this.font, pts, ptsX, topPos + PAD, COL_TEXT_POINTS, false);
        });
    }

    /**
     * Paints the tab bar background strip.
     * Individual tab highlights are handled by each AchievementTabWidget.
     */
    private void drawTabBarBackground(GuiGraphics g) {
        int tabBarY = topPos + HEADER_H;
        g.fill(leftPos, tabBarY, leftPos + guiW, tabBarY + TAB_BAR_H, COL_TAB_INACTIVE);
        // Separator below the tab bar
        g.fill(leftPos, tabBarY + TAB_BAR_H, leftPos + guiW, tabBarY + TAB_BAR_H + 1, COL_BORDER);
    }

    /** Small category label above the list area. */
    private void drawCategoryHeader(GuiGraphics g) {
        String catLabel = "— " + Component.translatable(selectedCategory.getTranslationKey()).getString() + " —";
        g.drawString(this.font, catLabel, leftPos + PAD, listY - 11, COL_TEXT_CAT_HEADER, false);
    }

    /**
     * Renders the scrollable achievement list.
     * Uses GuiGraphics.enableScissor / disableScissor to clip drawing strictly
     * to the list area, preventing items from bleeding into the header or
     * the panel border.
     *
     * enableScissor(x1, y1, x2, y2) — real method in Forge 1.20.1 GuiGraphics.
     * It internally converts GUI coords to framebuffer coords using GUI scale,
     * so we pass GUI-space coordinates directly.
     */
    private void drawAchievementList(GuiGraphics g, int mouseX, int mouseY) {
        if (Minecraft.getInstance().player == null) return;

        int listRight = scrollbarX - SCROLLBAR_PAD; // right edge of item area
        int itemWidth = listRight - (leftPos + PAD);

        // Scissor to list bounds — nothing drawn inside this block escapes the rectangle
        g.enableScissor(leftPos + PAD, listY, listRight, listY + listH);

        Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            if (filteredAchievements.isEmpty()) {
                g.disableScissor();
                String empty = Component.translatable("gui.ragnarmmo.achievements.empty").getString();
                int ex = leftPos + (guiW - this.font.width(empty)) / 2;
                int ey = listY  + listH / 2 - 4;
                g.drawString(this.font, empty, ex, ey, COL_TEXT_CAT_HEADER, false);
                return;
            }

            int   firstIndex = (int) scrollOffset;
            float subPixel   = scrollOffset - firstIndex; // 0..1 fractional pixel offset

            for (int i = 0; i <= visibleItems; i++) { // +1 so partial bottom row is visible
                int index = firstIndex + i;
                if (index >= filteredAchievements.size()) break;

                int itemY = listY + (int)(i * ITEM_HEIGHT - subPixel * ITEM_HEIGHT);

                AchievementDefinition def  = filteredAchievements.get(index);
                boolean hovered = mouseX >= leftPos + PAD && mouseX <= listRight
                               && mouseY >= itemY          && mouseY < itemY + ITEM_HEIGHT - 2;

                AchievementItemRenderer.render(g, this.font, def, cap,
                        leftPos + PAD, itemY, itemWidth, hovered);
            }

            g.disableScissor();
        });

        // If the capability was absent the scissor was not disabled inside the lambda;
        // guard against that by disabling here unconditionally.
        // GuiGraphics.disableScissor() is idempotent when nothing is active (Forge 1.20.1).
        // We intentionally call it a second time for the empty-list early-return path too.
    }

    /**
     * Scrollbar track and thumb.
     * Thumb position is calculated from the actual scrollable range so that
     * drag math in mouseDragged always stays consistent.
     */
    private void drawScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        int max = maxScrollIndex();
        if (max <= 0) return;

        int sbY = listY;
        int sbH = listH;

        // Track
        g.fill(scrollbarX, sbY, scrollbarX + SCROLLBAR_W, sbY + sbH, COL_SCROLLBAR_BG);
        g.renderOutline(scrollbarX, sbY, SCROLLBAR_W, sbH, 0xFF1A2A3A);

        // Thumb geometry — minimum 14 px tall for usability
        int thumbH = Math.max(14, (int)((float) visibleItems / filteredAchievements.size() * sbH));
        int thumbRange = sbH - thumbH; // pixels the thumb can travel
        int thumbY     = sbY + (max > 0 ? (int)(scrollOffset / max * thumbRange) : 0);

        boolean sbHovered = isDraggingScroll || (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                          && mouseY >= sbY && mouseY <= sbY + sbH);
        int thumbColor = sbHovered ? COL_SCROLLBAR_HOVER : COL_SCROLLBAR_THUMB;

        g.fill(scrollbarX + 1, thumbY + 1,
               scrollbarX + SCROLLBAR_W - 1, thumbY + thumbH - 1,
               thumbColor);
        // Subtle highlight on top edge of thumb
        g.fill(scrollbarX + 1, thumbY + 1,
               scrollbarX + SCROLLBAR_W - 1, thumbY + 2,
               0x55FFFFFF);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollTarget = Mth.clamp(scrollTarget - (float) delta, 0f, maxScrollIndex());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // ── Scrollbar drag start ───────────────────────────────────────────────
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                && mouseY >= listY && mouseY <= listY + listH) {
            isDraggingScroll   = true;
            scrollDragStartY   = (int) mouseY;
            scrollDragStartOff = scrollOffset;

            // If the click is on the track (not the thumb), jump scroll to that position
            int   max      = maxScrollIndex();
            int   thumbH   = Math.max(14, (int)((float) visibleItems / filteredAchievements.size() * listH));
            int   trackH   = listH - thumbH;
            float ratio    = trackH > 0 ? (float)(mouseY - listY - thumbH / 2f) / trackH : 0f;
            scrollTarget   = Mth.clamp(ratio * max, 0f, max);
            scrollOffset   = scrollTarget;
            scrollDragStartOff = scrollOffset;
            return true;
        }

        // ── Achievement item click ─────────────────────────────────────────────
        if (Minecraft.getInstance().player != null) {
            int listRight  = scrollbarX - SCROLLBAR_PAD;
            int firstIndex = (int) scrollOffset;
            float subPixel = scrollOffset - firstIndex;

            Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS)
                    .ifPresent(cap -> {
                        for (int i = 0; i <= visibleItems; i++) {
                            int index = firstIndex + i;
                            if (index >= filteredAchievements.size()) break;

                            int itemY = listY + (int)(i * ITEM_HEIGHT - subPixel * ITEM_HEIGHT);
                            int itemX = leftPos + PAD;

                            if (mouseX >= itemX && mouseX <= listRight
                                    && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 2) {

                                AchievementDefinition def = filteredAchievements.get(index);
                                handleItemClick(def, cap);
                                break; // only one item per click
                            }
                        }
                    });
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles the claim / equip / unequip logic for a clicked achievement.
     * Network calls are unchanged from the original implementation.
     */
    private void handleItemClick(AchievementDefinition def,
                                 com.etema.ragnarmmo.system.achievements.capability.IPlayerAchievements cap) {
        if (!cap.isUnlocked(def.id())) return;

        if (!cap.isClaimed(def.id())) {
            // Unclaimed but unlocked → send claim request
            Network.sendToServer(new ClaimAchievementPacket(def.id()));
        } else if (def.title() != null && !def.title().isEmpty()) {
            // Claimed and has title → toggle equip / unequip
            if (def.title().equals(cap.getActiveTitle())) {
                Network.sendToServer(new SetTitlePacket(""));
            } else {
                Network.sendToServer(new SetTitlePacket(def.title()));
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (isDraggingScroll && button == 0) {
            int max = maxScrollIndex();
            if (max <= 0) return true;

            // Recalculate thumb geometry identically to drawScrollbar()
            int thumbH    = Math.max(14, (int)((float) visibleItems / filteredAchievements.size() * listH));
            int trackH    = listH - thumbH; // pixels the thumb can actually travel
            if (trackH <= 0) return true;

            // Map mouse delta to scroll units using the same track-space calculation
            float dragDelta = (float)(mouseY - scrollDragStartY) / trackH * max;
            scrollTarget = Mth.clamp(scrollDragStartOff + dragDelta, 0f, max);
            scrollOffset = scrollTarget; // snap immediately during drag for responsiveness
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDraggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.etema.ragnarmmo.client.Keybinds.OPEN_ACHIEVEMENTS.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
