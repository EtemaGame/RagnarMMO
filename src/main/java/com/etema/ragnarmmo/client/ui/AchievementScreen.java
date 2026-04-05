package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.RagnarMMO;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AchievementScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    /** Fraction of the Minecraft window the GUI will occupy */
    private static final float WIDTH_FRACTION  = 0.75f;
    private static final float HEIGHT_FRACTION = 0.80f;

    /** Min / max clamped sizes so it never looks broken on extreme resolutions */
    private static final int MIN_WIDTH  = 300;
    private static final int MAX_WIDTH  = 480;
    private static final int MIN_HEIGHT = 220;
    private static final int MAX_HEIGHT = 360;

    /** Padding / spacing */
    private static final int PAD          = 8;
    private static final int HEADER_H     = 36;   // height of the top header bar
    private static final int TAB_BAR_H    = 22;   // height of the tab row
    private static final int ITEM_HEIGHT  = 44;   // height of each achievement row
    private static final int SCROLLBAR_W  = 6;

    // ── State ─────────────────────────────────────────────────────────────────
    private int guiW, guiH, leftPos, topPos;
    private int listY, listH;          // Y-start and pixel-height of the scrollable area
    private int visibleItems;

    private AchievementCategory selectedCategory = AchievementCategory.BASIC;
    private List<AchievementDefinition> filteredAchievements;

    // Smooth scroll
    private float scrollOffset     = 0f;   // fractional index offset (for smooth feel)
    private float scrollTarget     = 0f;
    private boolean isDraggingScroll = false;
    private int    scrollDragStartY  = 0;
    private float  scrollDragStartOffset = 0f;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int COL_BG           = 0xFF1A1A2E;
    private static final int COL_HEADER       = 0xFF16213E;
    private static final int COL_BORDER       = 0xFF0F3460;
    private static final int COL_ACCENT       = 0xFF533483;
    private static final int COL_TAB_ACTIVE   = 0xFF533483;
    private static final int COL_TAB_INACTIVE = 0xFF0F3460;
    private static final int COL_TAB_TEXT_ON  = 0xFFFFFFFF;
    private static final int COL_TAB_TEXT_OFF = 0xFF8899AA;

    private static final int COL_ITEM_LOCKED   = 0xFF111122;
    private static final int COL_ITEM_UNLOCKED = 0xFF1B3A2A;
    private static final int COL_ITEM_CLAIMED  = 0xFF163052;
    private static final int COL_ITEM_ACTIVE   = 0xFF2A1060;

    private static final int COL_TEXT_NAME      = 0xFFFFD700;
    private static final int COL_TEXT_LOCKED    = 0xFF556677;
    private static final int COL_TEXT_DESC      = 0xFFCCCCCC;
    private static final int COL_TEXT_POINTS    = 0xFFFFAA00;
    private static final int COL_TEXT_TITLE     = 0xFF55FFFF;
    private static final int COL_TEXT_TITLE_ACT = 0xFFFFFFFF;
    private static final int COL_TEXT_PROGRESS  = 0xFF888888;
    private static final int COL_TEXT_CLAIM     = 0xFF55FF55;
    private static final int COL_TEXT_CLAIMED   = 0xFF5588FF;
    private static final int COL_TEXT_EQUIP     = 0xFF55FF55;
    private static final int COL_TEXT_EQUIPPED  = 0xFF55FFFF;

    private static final int COL_SCROLLBAR_BG   = 0xFF0A0A18;
    private static final int COL_SCROLLBAR_THUMB = 0xFF533483;

    public AchievementScreen() {
        super(Component.translatable("gui.ragnarmmo.achievements.title"));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        recalculateLayout();
        refreshList();
        buildTabButtons();
        buildUnequipButton();
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        recalculateLayout();
        refreshList();
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private void recalculateLayout() {
        guiW = Mth.clamp((int)(this.width  * WIDTH_FRACTION),  MIN_WIDTH,  MAX_WIDTH);
        guiH = Mth.clamp((int)(this.height * HEIGHT_FRACTION), MIN_HEIGHT, MAX_HEIGHT);

        leftPos = (this.width  - guiW) / 2;
        topPos  = (this.height - guiH) / 2;

        listY = topPos + HEADER_H + TAB_BAR_H + PAD;
        listH = guiH - HEADER_H - TAB_BAR_H - PAD * 2;
        visibleItems = Math.max(1, listH / ITEM_HEIGHT);
    }

    private void buildTabButtons() {
        // Remove old tab buttons before re-adding
        clearWidgets();

        AchievementCategory[] cats = AchievementCategory.values();
        int totalTabs = cats.length;

        // Distribute tab widths evenly across the full GUI width
        int totalTabW = guiW - PAD * 2;
        int tabH      = TAB_BAR_H - 2;
        int tabY      = topPos + HEADER_H + 2;

        for (int i = 0; i < totalTabs; i++) {
            final AchievementCategory cat = cats[i];

            // Integer-split to avoid gaps/overlap
            int x0 = leftPos + PAD + (totalTabW * i)      / totalTabs;
            int x1 = leftPos + PAD + (totalTabW * (i+1))  / totalTabs;
            int tw = x1 - x0 - 1; // 1-px gap between tabs

            this.addRenderableWidget(
                Button.builder(Component.translatable(cat.getTranslationKey()), b -> {
                    selectedCategory = cat;
                    scrollOffset = 0f;
                    scrollTarget = 0f;
                    refreshList();
                    // Rebuild so active-tab style updates
                    buildTabButtons();
                    buildUnequipButton();
                })
                .bounds(x0, tabY, tw, tabH)
                .build()
            );
        }
    }

    private void buildUnequipButton() {
        int btnW = 80;
        int btnH = 14;
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.ragnarmmo.achievements.unequip"), b ->
                Network.sendToServer(new SetTitlePacket(""))
            )
            .bounds(leftPos + guiW - btnW - PAD, topPos + (HEADER_H - btnH) / 2, btnW, btnH)
            .build()
        );
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshList() {
        filteredAchievements = AchievementRegistry.getInstance().getAll().values().stream()
                .filter(a -> a.category() == selectedCategory)
                .sorted(Comparator.comparing(AchievementDefinition::id))
                .collect(Collectors.toList());
        // Clamp scroll
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
        // Smooth scroll interpolation (lerp)
        scrollOffset += (scrollTarget - scrollOffset) * 0.25f;

        this.renderBackground(g);
        drawPanel(g);
        drawHeader(g);
        drawTabBar(g);
        drawAchievementList(g, mouseX, mouseY, partialTick);
        drawScrollbar(g);

        // Widgets (buttons) on top
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Main panel background */
    private void drawPanel(GuiGraphics g) {
        // Drop shadow
        g.fill(leftPos + 4, topPos + 4, leftPos + guiW + 4, topPos + guiH + 4, 0x88000000);
        // Background
        g.fill(leftPos, topPos, leftPos + guiW, topPos + guiH, COL_BG);
        // Outer border
        g.renderOutline(leftPos, topPos, guiW, guiH, COL_BORDER);
        // Inner accent line just inside the border
        g.fill(leftPos + 1, topPos + 1, leftPos + guiW - 1, topPos + 2, COL_ACCENT);
        g.fill(leftPos + 1, topPos + guiH - 2, leftPos + guiW - 1, topPos + guiH - 1, COL_ACCENT);
    }

    /** Header bar with title, active title and points */
    private void drawHeader(GuiGraphics g) {
        // Header background
        g.fill(leftPos, topPos, leftPos + guiW, topPos + HEADER_H, COL_HEADER);
        // Separator line
        g.fill(leftPos, topPos + HEADER_H - 1, leftPos + guiW, topPos + HEADER_H, COL_ACCENT);

        // Screen title
        g.drawString(this.font, "✦ " + Component.translatable("gui.ragnarmmo.achievements.title").getString(), leftPos + PAD, topPos + PAD, COL_TEXT_NAME, false);

        if (Minecraft.getInstance().player == null) return;

        Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            // Active title
            String activeTitle = cap.getActiveTitle();
            if (activeTitle != null && !activeTitle.isEmpty()) {
                String titleText = "[" + Component.translatable(activeTitle).getString() + "]";
                int tx = leftPos + guiW / 2 - this.font.width(titleText) / 2;
                g.drawString(this.font, titleText, tx, topPos + PAD, COL_TEXT_TITLE, false);
            }

            // Points — right-aligned, leave room for Desequipar button
            String pts = "⭐ " + cap.getTotalPoints() + " pts";
            int px = leftPos + guiW - 90 - this.font.width(pts); // 90 = button width + pad
            g.drawString(this.font, pts, px, topPos + PAD, COL_TEXT_POINTS, false);
        });
    }

    /** Tab bar (visual highlight; buttons drawn by super.render) */
    private void drawTabBar(GuiGraphics g) {
        int tabBarY = topPos + HEADER_H;
        g.fill(leftPos, tabBarY, leftPos + guiW, tabBarY + TAB_BAR_H, COL_TAB_INACTIVE);

        // Highlight active tab background
        AchievementCategory[] cats = AchievementCategory.values();
        int totalTabs = cats.length;
        int totalTabW = guiW - PAD * 2;
        int tabH      = TAB_BAR_H - 2;

        for (int i = 0; i < totalTabs; i++) {
            if (cats[i] == selectedCategory) {
                int x0 = leftPos + PAD + (totalTabW * i)     / totalTabs;
                int x1 = leftPos + PAD + (totalTabW * (i+1)) / totalTabs - 1;
                g.fill(x0, tabBarY + 2, x1, tabBarY + TAB_BAR_H, COL_TAB_ACTIVE);
                // Bottom accent line for active tab
                g.fill(x0, tabBarY + TAB_BAR_H - 2, x1, tabBarY + TAB_BAR_H, 0xFFFFD700);
                break;
            }
        }

        // Separator below tab bar
        g.fill(leftPos, tabBarY + TAB_BAR_H, leftPos + guiW, tabBarY + TAB_BAR_H + 1, COL_BORDER);
    }

    /** List of achievement items */
    private void drawAchievementList(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().player == null) return;

        // Clip drawing to the list area
        int listRight = leftPos + guiW - SCROLLBAR_W - PAD;

        Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            // Category header
            g.drawString(this.font,
                    "— " + Component.translatable(selectedCategory.getTranslationKey()).getString() + " —",
                    leftPos + PAD, listY - 12, COL_TAB_TEXT_OFF, false);

            // Empty state
            if (filteredAchievements.isEmpty()) {
                String empty = Component.translatable("gui.ragnarmmo.achievements.empty").getString();
                int ex = leftPos + (guiW - this.font.width(empty)) / 2;
                int ey = listY + listH / 2 - 4;
                g.drawString(this.font, empty, ex, ey, COL_TEXT_PROGRESS, false);
                return;
            }

            int firstIndex = (int) scrollOffset;
            float subPixel = scrollOffset - firstIndex;   // 0..1 fractional offset

            for (int i = 0; i < visibleItems + 1; i++) {  // +1 for partial last row
                int index = firstIndex + i;
                if (index >= filteredAchievements.size()) break;

                int itemY = listY + (int)(i * ITEM_HEIGHT - subPixel * ITEM_HEIGHT);

                // Only render if item is at least partially within the list area
                if (itemY + ITEM_HEIGHT < listY || itemY > listY + listH) continue;

                AchievementDefinition def = filteredAchievements.get(index);
                boolean hovered = mouseX >= leftPos + PAD && mouseX <= listRight
                        && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2;

                renderAchievementItem(g, def, cap, leftPos + PAD, itemY, listRight, hovered);
            }
        });
    }

    private void renderAchievementItem(GuiGraphics g,
                                       AchievementDefinition def,
                                       com.etema.ragnarmmo.system.achievements.capability.IPlayerAchievements cap,
                                       int x, int y, int xRight, boolean hovered) {

        boolean unlocked    = cap.isUnlocked(def.id());
        boolean claimed     = cap.isClaimed(def.id());
        boolean hasTitle    = def.title() != null && !def.title().isEmpty();
        boolean isActive    = hasTitle && def.title().equals(cap.getActiveTitle());

        int itemW = xRight - x;

        // Background
        int bgColor = isActive ? COL_ITEM_ACTIVE
                    : claimed  ? COL_ITEM_CLAIMED
                    : unlocked ? COL_ITEM_UNLOCKED
                               : COL_ITEM_LOCKED;

        if (hovered) bgColor = blendColor(bgColor, 0xFFFFFFFF, 0.06f);
        g.fill(x, y, x + itemW, y + ITEM_HEIGHT - 2, bgColor);

        // Left accent stripe
        int stripeColor = isActive ? 0xFFAA55FF
                        : claimed  ? 0xFF5588FF
                        : unlocked ? 0xFF55FF55
                                   : 0xFF334455;
        g.fill(x, y, x + 2, y + ITEM_HEIGHT - 2, stripeColor);

        // Border
        int borderColor = isActive ? 0xFF7733CC : hovered ? 0xFF667799 : 0xFF2A3A50;
        g.renderOutline(x, y, itemW, ITEM_HEIGHT - 2, borderColor);

        // Name
        int nameColor = unlocked ? COL_TEXT_NAME : COL_TEXT_LOCKED;
        g.drawString(this.font, Component.translatable(def.name()), x + 6, y + 4, nameColor, false);

        // Description
        g.drawString(this.font, Component.translatable(def.description()), x + 6, y + 14, COL_TEXT_DESC, false);

        // Title line
        if (hasTitle) {
            int tc = isActive ? COL_TEXT_TITLE_ACT : COL_TEXT_TITLE;
            String translatedTitle = Component.translatable(def.title()).getString();
            String titleLine = (isActive ? "★ " : "○ ") + "[" + translatedTitle + "]";
            g.drawString(this.font, titleLine, x + 6, y + 24, tc, false);
        }

        // Right-side status / progress
        String statusText;
        int    statusColor;

        if (!unlocked && !def.triggerType().equals("level_up")) {
            int progress = cap.getProgress(def.id() + "_progress");
            statusText  = progress + " / " + def.requiredAmount();
            statusColor = COL_TEXT_PROGRESS;

            // Mini progress bar
            int barW    = 55;
            int barX    = x + itemW - barW - 4;
            int barY    = y + ITEM_HEIGHT - 16;
            float pct   = def.requiredAmount() > 0
                    ? Mth.clamp((float) progress / def.requiredAmount(), 0f, 1f) : 0f;
            g.fill(barX, barY, barX + barW, barY + 4, 0xFF111111);
            g.fill(barX, barY, barX + (int)(barW * pct), barY + 4, 0xFF33AA55);
            g.renderOutline(barX, barY, barW, 4, 0xFF334455);

        } else if (unlocked && !claimed) {
            statusText  = Component.translatable("gui.ragnarmmo.achievements.reclaim").getString();
            statusColor = COL_TEXT_CLAIM;
        } else if (claimed) {
            if (hasTitle) {
                if (isActive) {
                    statusText  = Component.translatable("gui.ragnarmmo.achievements.equipped").getString();
                    statusColor = COL_TEXT_EQUIPPED;
                } else {
                    statusText  = Component.translatable("gui.ragnarmmo.achievements.equip").getString();
                    statusColor = COL_TEXT_EQUIP;
                }
            } else {
                statusText  = Component.translatable("gui.ragnarmmo.achievements.claimed").getString();
                statusColor = COL_TEXT_CLAIMED;
            }
        } else {
            statusText  = "";
            statusColor = 0;
        }

        if (!statusText.isEmpty()) {
            int sw = this.font.width(statusText);
            g.drawString(this.font, statusText, x + itemW - sw - 4, y + 4, statusColor, false);
        }

        // Points badge (top-right corner)
        if (def.points() > 0) {
            String pts = "+" + def.points();
            int pw = this.font.width(pts);
            g.drawString(this.font, pts, x + itemW - pw - 4, y + 14, COL_TEXT_POINTS, false);
        }
    }

    /** Scrollbar on the right edge */
    private void drawScrollbar(GuiGraphics g) {
        int max = maxScrollIndex();
        if (max <= 0) return;

        int sbX = leftPos + guiW - SCROLLBAR_W - 2;
        int sbY = listY;
        int sbH = listH;

        // Track
        g.fill(sbX, sbY, sbX + SCROLLBAR_W, sbY + sbH, COL_SCROLLBAR_BG);

        // Thumb
        float thumbH   = (float) visibleItems / filteredAchievements.size() * sbH;
        thumbH = Math.max(12, thumbH);
        float thumbY   = sbY + (scrollOffset / max) * (sbH - thumbH);
        g.fill(sbX, (int) thumbY, sbX + SCROLLBAR_W, (int)(thumbY + thumbH), COL_SCROLLBAR_THUMB);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollTarget = Mth.clamp(scrollTarget - (float) delta, 0f, maxScrollIndex());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check scrollbar drag start
        int sbX = leftPos + guiW - SCROLLBAR_W - 2;
        if (button == 0 && mouseX >= sbX && mouseX <= sbX + SCROLLBAR_W
                && mouseY >= listY && mouseY <= listY + listH) {
            isDraggingScroll  = true;
            scrollDragStartY  = (int) mouseY;
            scrollDragStartOffset = scrollOffset;
            return true;
        }

        // Click on an achievement item
        if (button == 0 && Minecraft.getInstance().player != null) {
            int firstIndex = (int) scrollOffset;
            float subPixel = scrollOffset - firstIndex;
            int listRight  = leftPos + guiW - SCROLLBAR_W - PAD;

            Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS)
                    .ifPresent(cap -> {
                        for (int i = 0; i < visibleItems + 1; i++) {
                            int index = firstIndex + i;
                            if (index >= filteredAchievements.size()) break;

                            int itemY = listY + (int)(i * ITEM_HEIGHT - subPixel * ITEM_HEIGHT);
                            int itemX = leftPos + PAD;

                            if (mouseX >= itemX && mouseX <= listRight
                                     && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2) {

                                AchievementDefinition def = filteredAchievements.get(index);
                                if (cap.isUnlocked(def.id())) {
                                    if (!cap.isClaimed(def.id())) {
                                        Network.sendToServer(new ClaimAchievementPacket(def.id()));
                                    } else if (def.title() != null && !def.title().isEmpty()) {
                                        if (def.title().equals(cap.getActiveTitle())) {
                                            Network.sendToServer(new SetTitlePacket(""));
                                        } else {
                                            Network.sendToServer(new SetTitlePacket(def.title()));
                                        }
                                    }
                                }
                            }
                        }
                    });
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScroll && button == 0 && maxScrollIndex() > 0) {
            int max   = maxScrollIndex();
            float pxPerUnit = (float) listH / (filteredAchievements.size());
            float delta = (float)(mouseY - scrollDragStartY) / pxPerUnit;
            scrollTarget = Mth.clamp(scrollDragStartOffset + delta, 0f, max);
            scrollOffset = scrollTarget;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDraggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Linearly blends two ARGB colors.
     * @param t 0.0 = 100% base, 1.0 = 100% overlay
     */
    private static int blendColor(int base, int overlay, float t) {
        int aB = (base    >> 24) & 0xFF, rB = (base    >> 16) & 0xFF,
            gB = (base    >>  8) & 0xFF, bB =  base           & 0xFF;
        int aO = (overlay >> 24) & 0xFF, rO = (overlay >> 16) & 0xFF,
            gO = (overlay >>  8) & 0xFF, bO =  overlay         & 0xFF;
        int a = (int)(aB + (aO - aB) * t);
        int r = (int)(rB + (rO - rB) * t);
        int gg= (int)(gB + (gO - gB) * t);
        int b = (int)(bB + (bO - bB) * t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
