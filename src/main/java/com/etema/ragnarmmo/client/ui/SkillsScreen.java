package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.ui.SkillTreeAdapter.SkillNodeWrapper;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.skill.api.SkillTier;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.stats.net.PacketUpgradeSkill;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillProgress;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * SkillsScreen - RO-style Skill Tree UI.
 * FIXED: Proper panel scaling, unified local coordinate system, correct
 * hover/click mapping,
 * manual buttons (so they scale), and tooltips rendered outside scaled pose.
 *
 * EXTRA FIX: footer button layout computed dynamically to avoid overlap:
 * Back vs Reset/Apply.
 */
public class SkillsScreen extends Screen {

    private final Screen parent;

    // === LAYOUT CONSTANTS (panel-local units) ===
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;

    // Tabs
    private static final int TAB_WIDTH = 60;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 4;

    // Skill nodes
    private static final int CELL_SIZE = 32;
    private static final int CELL_SPACING = 8;
    private static final int ICON_SIZE = 24;

    // Buttons (panel-local)
    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 20;

    // Footer layout
    private static final int FOOTER_Y = PANEL_HEIGHT - 28;
    private static final int FOOTER_MARGIN = 8;
    private static final int FOOTER_GAP = 6;

    // Runtime button rectangles (panel-local)
    private Rect backRect;
    private Rect resetRect;
    private Rect applyRect;

    // Static rects that don't depend on footer layout
    private static final Rect BTN_CHANGE_CLASS = new Rect(PANEL_WIDTH - 84, 6, 76, 16);

    // Dynamic tab system — tabs are rebuilt when job changes
    // Helper class for UI tabs
    private static class TabEntry {
        final int tier; // -1 for Life, 0 for Novice, 1 for 1st class, 2 for 2nd class
        final String id;
        final JobType job; // The JobType associated with this tab

        TabEntry(int tier, String id, JobType job) {
            this.tier = tier;
            this.id = id;
            this.job = job;
        }

        boolean isLifeTab() {
            return job == null && id.equals(Component.translatable("screen.ragnarmmo.skills.tab.life").getString());
        }

        boolean isSkillTab() {
            return !isLifeTab();
        }
    }

    // Allocation System
    private final Map<ResourceLocation, Integer> pendingUpgrades = new HashMap<>();

    // Tab state
    private List<TabEntry> activeTabs = new ArrayList<>();
    private int activeTabIndex = 0;

    // Cached visible skills for current tab
    private List<SkillNodeWrapper> visibleSkills = new ArrayList<>();
    private List<LifeSkillType> visibleLifeSkills = new ArrayList<>();

    // Track current job to detect changes
    private String lastKnownJobId = "";

    // === Scaled panel placement ===
    private float uiScale = 1.0f;
    private float scrollOffset = 0.0f;
    private float maxScroll = 0.0f;
    private int panelX = 0; // screen coords
    private int panelY = 0; // screen coords

    // Tooltip deferred render (tooltip must be drawn OUTSIDE scaled pose)
    private List<Component> deferredTooltip = null;

    // Simple rect helper
    private static final class Rect {
        final int x, y, w, h;

        Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    public SkillsScreen(Screen parent) {
        super(Component.translatable("screen.ragnarmmo.skills.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        recalcPanelTransform();
        rebuildTabs();
        updateVisibleSkills();
        updateLastKnownJobId();
    }

    @Override
    public void tick() {
        super.tick();
        checkForJobChange();
    }

    private void updateLastKnownJobId() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                this.lastKnownJobId = stats.getJobId();
            });
        }
    }

    private void checkForJobChange() {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return;

        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            String currentJobId = stats.getJobId();
            if (!currentJobId.equals(lastKnownJobId)) {
                // Job changed! Rebuild tabs, update visible skills, clear pending
                lastKnownJobId = currentJobId;
                rebuildTabs();
                updateVisibleSkills();
                pendingUpgrades.clear();
            }
        });
    }

    /**
     * Builds the tab list based on the player's current job.
     * Novice: [Novice] [Life]
     * 1st class (e.g. Mage): [Novice] [Mage] [Wizard] [Life]
     * 2nd class (e.g. Wizard): [Novice] [Mage] [Wizard] [Life]
     */
    private void rebuildTabs() {
        List<TabEntry> tabs = new ArrayList<>();

        // 1. Always: Novice Tab
        tabs.add(new TabEntry(0, Component.translatable("screen.ragnarmmo.skills.tab.novice").getString(),
                JobType.NOVICE));

        var player = Minecraft.getInstance().player;
        if (player != null) {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                JobType job = JobType.fromId(stats.getJobId());

                if (job != JobType.NOVICE) {
                    JobType firstClass = job;
                    // If current job is 2nd class, its parent is the 1st class
                    if (job.getTier() == 2 && job.getParent() != null) {
                        firstClass = job.getParent();
                    }

                    // 2. Add 1st Class Tab
                    tabs.add(new TabEntry(1, Component.translatable("job.ragnarmmo." + firstClass.getId()).getString(),
                            firstClass));

                    // 3. Add 2nd Class Tab ONLY if the player is currently that 2nd class
                    if (job.getTier() == 2) {
                        tabs.add(new TabEntry(2, Component.translatable("job.ragnarmmo." + job.getId()).getString(),
                                job));
                    }
                }
            });
        }

        // 4. Always: Life Tab
        tabs.add(new TabEntry(-1, Component.translatable("screen.ragnarmmo.skills.tab.life").getString(), null));

        this.activeTabs = tabs;

        // Keep active tab index in range
        if (activeTabIndex >= activeTabs.size()) {
            activeTabIndex = 0;
        }
    }

    private TabEntry getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < activeTabs.size()) {
            return activeTabs.get(activeTabIndex);
        }
        return activeTabs.isEmpty() ? new TabEntry(0, "Novice", JobType.NOVICE) : activeTabs.get(0);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        recalcPanelTransform();
    }

    private void recalcPanelTransform() {
        // Scale DOWN only. If you want scale-up, remove the min(1.0f, ...)
        float sx = (float) this.width / (float) PANEL_WIDTH;
        float sy = (float) this.height / (float) PANEL_HEIGHT;
        this.uiScale = Math.min(1.0f, Math.min(sx, sy));

        int scaledW = Math.round(PANEL_WIDTH * uiScale);
        int scaledH = Math.round(PANEL_HEIGHT * uiScale);

        this.panelX = (this.width - scaledW) / 2;
        this.panelY = (this.height - scaledH) / 2;
    }

    public SkillNodeWrapper getHoveredSkill(double mouseX, double mouseY) {
        TabEntry tab = getActiveTab();
        if (!tab.isSkillTab())
            return null;

        double mxLocal = (mouseX - panelX) / uiScale;
        double myLocal = (mouseY - panelY) / uiScale;
        
        // Add scrollOffset to mouse Y to match the grid coordinates (startY + gridY * 40)
        double scrolledY = myLocal + scrollOffset;

        int startX = 16;
        int startY = 54;

        for (SkillNodeWrapper wrapper : visibleSkills) {
            int x = startX + wrapper.getGridX() * (32 + 8); // CELL_SIZE + SPACING
            int y = startY + wrapper.getGridY() * (32 + 8);

            if (mxLocal >= x && mxLocal < x + 32 && scrolledY >= y && scrolledY < y + 32) {
                return wrapper;
            }
        }
        return null;
    }

    private boolean isRegularTab() {
        return getActiveTab().isSkillTab();
    }

    private boolean applyEnabled(net.minecraft.world.entity.player.Player player) {
        if (!isRegularTab())
            return false;
        if (pendingUpgrades.isEmpty())
            return false;

        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty())
            return false;

        int originalPoints = statsOpt.get().getSkillPoints();
        int spentPoints = pendingUpgrades.values().stream().mapToInt(Integer::intValue).sum();
        return (originalPoints - spentPoints) >= 0;
    }

    private boolean resetEnabled() {
        return isRegularTab() && !pendingUpgrades.isEmpty();
    }

    private void applyChanges() {
        if (pendingUpgrades.isEmpty())
            return;

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        PlayerSkillsProvider.get(player).ifPresent(skillManager -> {
            List<ResourceLocation> packetOrder = buildUpgradePacketOrder(skillManager);
            if (packetOrder.isEmpty()) {
                return;
            }

            Map<ResourceLocation, Integer> scheduled = new HashMap<>();
            for (ResourceLocation skillId : packetOrder) {
                Network.sendToServer(new PacketUpgradeSkill(skillId));
                scheduled.merge(skillId, 1, Integer::sum);
            }

            scheduled.forEach((skillId, sentCount) -> pendingUpgrades.computeIfPresent(skillId,
                    (id, pending) -> pending > sentCount ? pending - sentCount : null));
        });
    }

    private List<ResourceLocation> buildUpgradePacketOrder(
            com.etema.ragnarmmo.skill.runtime.SkillManager skillManager) {
        Map<ResourceLocation, Integer> remaining = new HashMap<>(pendingUpgrades);
        Map<ResourceLocation, Integer> simulatedLevels = new HashMap<>();
        for (ResourceLocation skillId : remaining.keySet()) {
            simulatedLevels.put(skillId, skillManager.getSkillLevel(skillId));
        }

        List<ResourceLocation> preferredOrder = new ArrayList<>();
        for (SkillNodeWrapper wrapper : visibleSkills) {
            preferredOrder.add(wrapper.getSkillId());
        }
        remaining.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(skillId -> {
                    if (!preferredOrder.contains(skillId)) {
                        preferredOrder.add(skillId);
                    }
                });

        List<ResourceLocation> packetOrder = new ArrayList<>();
        boolean progressed = true;
        while (!remaining.isEmpty() && progressed) {
            progressed = false;

            for (ResourceLocation skillId : preferredOrder) {
                int pending = remaining.getOrDefault(skillId, 0);
                if (pending <= 0) {
                    continue;
                }

                var defOpt = SkillRegistry.get(skillId);
                if (defOpt.isEmpty()) {
                    continue;
                }

                boolean requirementsMet = true;
                for (var requirement : defOpt.get().getRequirements().entrySet()) {
                    ResourceLocation reqId = requirement.getKey();
                    int reqLevel = requirement.getValue();
                    int currentLevel = simulatedLevels.containsKey(reqId)
                            ? simulatedLevels.get(reqId)
                            : skillManager.getSkillLevel(reqId);
                    if (currentLevel < reqLevel) {
                        requirementsMet = false;
                        break;
                    }
                }

                if (!requirementsMet) {
                    continue;
                }

                packetOrder.add(skillId);
                simulatedLevels.put(skillId, simulatedLevels.getOrDefault(skillId, skillManager.getSkillLevel(skillId)) + 1);
                if (pending == 1) {
                    remaining.remove(skillId);
                } else {
                    remaining.put(skillId, pending - 1);
                }
                progressed = true;
            }
        }

        return packetOrder;
    }

    /**
     * Central method: get visible skills based on tab entry.
     * Uses the dynamic skill tree system via SkillTreeAdapter.
     */
    private List<SkillNodeWrapper> getVisibleSkillsForTab(JobType playerJob, TabEntry tab) {
        if (tab.isLifeTab()) {
            return new ArrayList<>();
        }

        // For tier 0 (Novice), always use SkillTreeAdapter with tier=0
        if (tab.tier == 0) {
            return SkillTreeAdapter.getVisibleSkills(playerJob, 0);
        }

        // For class tabs (1st/2nd), use the tab's job for the query
        JobType queryJob = tab.job != null ? tab.job : playerJob;
        return SkillTreeAdapter.getVisibleSkills(queryJob, tab.tier);
    }

    private void updateVisibleSkills() {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return;

        TabEntry tab = getActiveTab();

        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            JobType job = JobType.fromId(stats.getJobId());
            this.visibleSkills = getVisibleSkillsForTab(job, tab);
        });

        if (tab.isLifeTab()) {
            this.visibleLifeSkills = Arrays.asList(LifeSkillType.values());
        } else {
            this.visibleLifeSkills = Collections.emptyList();
        }
    }

    /**
     * Compute footer button rectangles so they never overlap.
     * - If Apply/Reset are shown, Back goes left.
     * - Otherwise Back is centered.
     */
    private void layoutFooterButtons(boolean showApplyReset) {
        if (!showApplyReset) {
            // Back centrado cuando está solo
            int backW = 80;
            int backH = 20;
            this.backRect = new Rect((PANEL_WIDTH - backW) / 2, FOOTER_Y, backW, backH);
            this.resetRect = null;
            this.applyRect = null;
            return;
        }

        // Apply pegado a la derecha (como lo tenías)
        int applyX = PANEL_WIDTH - FOOTER_MARGIN - BUTTON_WIDTH;
        this.applyRect = new Rect(applyX, FOOTER_Y, BUTTON_WIDTH, BUTTON_HEIGHT);

        // Reset a la izquierda de Apply
        int resetX = applyX - FOOTER_GAP - BUTTON_WIDTH;
        this.resetRect = new Rect(resetX, FOOTER_Y, BUTTON_WIDTH, BUTTON_HEIGHT);

        // Back "casi centrado" pero evitando solape con Reset.
        // Para que no se vaya al extremo, lo hacemos un poco más angosto solo en este
        // modo.
        int backW = 60; // <- ajuste clave (antes 80)
        int backH = 20;

        int centeredX = (PANEL_WIDTH - backW) / 2;
        int maxXBeforeReset = resetX - FOOTER_GAP - backW; // backRight <= resetX - gap
        int backX = Math.min(centeredX, maxXBeforeReset);

        // opcional: no dejar que se pegue demasiado al borde
        backX = Math.max(FOOTER_MARGIN, backX);

        this.backRect = new Rect(backX, FOOTER_Y, backW, backH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        this.deferredTooltip = null;

        // Convert mouse to panel-local coordinates
        double mxLocal = (mouseX - panelX) / uiScale;
        double myLocal = (mouseY - panelY) / uiScale;

        // Draw scaled panel
        g.pose().pushPose();
        g.pose().translate(panelX, panelY, 0);
        g.pose().scale(uiScale, uiScale, 1.0f);

        // Panel background (LOCAL coords)
        g.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BG);
        g.renderOutline(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BORDER);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            renderHeaderLocal(g, player);
        }

        renderTabsLocal(g, mxLocal, myLocal);

        // Content Area with Scissor and Scroll
        int contentX = 10;
        int contentY = 40;
        int contentWidth = PANEL_WIDTH - 20;
        int contentHeight = PANEL_HEIGHT - 75;

        // Scissor box (Screen-space coordinates)
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int scissorX = (int) ((panelX + contentX * uiScale) * scale);
        int scissorY = (int) (Minecraft.getInstance().getWindow().getHeight() - (panelY + (contentY + contentHeight) * uiScale) * scale);
        int scissorW = (int) (contentWidth * uiScale * scale);
        int scissorH = (int) (contentHeight * uiScale * scale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        g.pose().pushPose();
        g.pose().translate(0, -scrollOffset, 0);

        // Content
        TabEntry currentTab = getActiveTab();
        if (currentTab.isLifeTab()) {
            if (player != null) {
                var lifeManager = com.etema.ragnarmmo.system.lifeskills.LifeSkillCapability.get(player).orElse(null);
                if (lifeManager != null) {
                    renderLifeSkillTreeLocal(g, lifeManager, 16, 54, mxLocal, myLocal + scrollOffset);
                    // compute tooltip in LOCAL; render later
                    this.deferredTooltip = buildLifeTooltipLocal(lifeManager, 16, 54, mxLocal, myLocal + scrollOffset);
                }
            }
        } else {
            if (player != null) {
                PlayerSkillsProvider.get(player).ifPresent(skillManager -> {
                    com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                        JobType job = JobType.fromId(stats.getJobId());
                        renderSkillTreeLocal(g, skillManager, job, 16, 54, mxLocal, myLocal + scrollOffset);
                        this.deferredTooltip = buildSkillTooltipLocal(skillManager, 16, 54, mxLocal, myLocal + scrollOffset);
                    });
                });
            }
        }

        g.pose().popPose();
        RenderSystem.disableScissor();

        if (currentTab.isLifeTab()) {
            if (player != null) {
                var lifeManager = com.etema.ragnarmmo.system.lifeskills.LifeSkillCapability.get(player).orElse(null);
                if (lifeManager != null) {
                    renderLifeFooterInfoLocal(g, lifeManager);
                }
            }
        } else if (player != null) {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> renderFooterInfoLocal(g, stats));
        }

        // Manual buttons (LOCAL)
        renderButtonsLocal(g, player, mxLocal, myLocal);

        g.pose().popPose();

        // Tooltips OUTSIDE scaled pose so they don't get scaled/distorted
        if (deferredTooltip != null && !deferredTooltip.isEmpty()) {
            g.renderComponentTooltip(this.font, deferredTooltip, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHeaderLocal(GuiGraphics g, net.minecraft.world.entity.player.Player player) {
        int titleX = 8;
        int titleY = 8;

        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            JobType job = JobType.fromId(stats.getJobId());
            Component titleText = Component.translatable("screen.ragnarmmo.skills.header", job.getDisplayName());
            g.drawString(this.font, titleText, titleX, titleY, GuiConstants.COLOR_TITLE, true);
        });
    }

    private void renderTabsLocal(GuiGraphics g, double mx, double my) {
        // Draw Tab labels/icons
        int tabY = -28;
        int tabWidth = 65;
        for (int i = 0; i < activeTabs.size(); i++) {
            TabEntry tab = activeTabs.get(i);
            int tx = 10 + (i * 70); // spaced out tabs

            boolean isActive = (i == activeTabIndex);
            boolean isHovered = mx >= tx && mx < tx + tabWidth && my >= tabY && my < tabY + 28;

            // Background & Border
            int bgColor = isActive ? 0xFF505050 : (isHovered ? 0xFF404040 : 0xFF252525);
            g.fill(tx, tabY, tx + tabWidth, tabY + 28, bgColor);
            int borderColor = isActive ? 0xFFFFAA00 : GuiConstants.COLOR_PANEL_BORDER;
            g.renderOutline(tx, tabY, tabWidth, 28, borderColor);

            // Text color
            int textColor = isActive ? 0xFFFFFFFF : (isHovered ? 0xFFCCCCCC : 0xFF888888);

            // Use tab.job or translatable name
            String displayText = tab.id.startsWith("screen.ragnarmmo") || tab.id.startsWith("job.ragnarmmo")
                    ? Component.translatable(tab.id).getString()
                    : tab.id;
            g.drawCenteredString(this.font, displayText, tx + (tabWidth / 2), tabY + 10, textColor);
        }
    }

    private void renderButtonsLocal(GuiGraphics g, net.minecraft.world.entity.player.Player player, double mx,
            double my) {
        boolean showApplyReset = isRegularTab() && player != null;
        layoutFooterButtons(showApplyReset);

        // Back always
        drawButtonLocal(g, backRect, Component.translatable("screen.ragnarmmo.button.back"), backRect.contains(mx, my),
                true);

        if (showApplyReset) {
            boolean applyActive = applyEnabled(player);
            boolean resetActive = resetEnabled();

            drawButtonLocal(g, resetRect, Component.translatable("screen.ragnarmmo.button.reset"),
                    resetRect.contains(mx, my), resetActive);
            drawButtonLocal(g, applyRect, Component.translatable("screen.ragnarmmo.button.apply"),
                    applyRect.contains(mx, my), applyActive);
        }

        if (player != null) {
            com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
                if (JobType.fromId(stats.getJobId()).hasPromotions()) {
                    drawButtonLocal(g, BTN_CHANGE_CLASS, Component.translatable("screen.ragnarmmo.button.change_class"),
                            BTN_CHANGE_CLASS.contains(mx, my), true);
                }
            });
        }
    }

    private void drawButtonLocal(GuiGraphics g, Rect r, Component label, boolean hovered, boolean enabled) {
        int bg;
        if (!enabled)
            bg = 0xFF2A2A2A;
        else
            bg = hovered ? 0xFF505050 : 0xFF3A3A3A;

        int border = hovered ? 0xFFFFAA00 : GuiConstants.COLOR_PANEL_BORDER;
        int text = enabled ? (hovered ? 0xFFFFFFFF : 0xFFDDDDDD) : 0xFF777777;

        g.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
        g.renderOutline(r.x, r.y, r.w, r.h, border);

        int tw = this.font.width(label);
        int tx = r.x + (r.w - tw) / 2;
        int ty = r.y + (r.h - 8) / 2; // 8 ~= font height baseline
        g.drawString(this.font, label, tx, ty, text, false);
    }

    private void renderSkillTreeLocal(GuiGraphics g, com.etema.ragnarmmo.skill.runtime.SkillManager skillManager,
            JobType job, int startX, int startY, double mx, double my) {

        List<SkillNodeWrapper> skills = visibleSkills;
        int furthestY = 0;
        for (SkillNodeWrapper wrapper : skills) {
             int y = startY + wrapper.getGridY() * (CELL_SIZE + CELL_SPACING);
             furthestY = Math.max(furthestY, y + CELL_SIZE);
        }
        maxScroll = Math.max(0, furthestY + 20 - (PANEL_HEIGHT - 65));

        Map<ResourceLocation, SkillNodeWrapper> visibleMap = new HashMap<>();
        for (SkillNodeWrapper wrapper : skills) {
            visibleMap.put(wrapper.getSkillId(), wrapper);
        }

        // Connectors first
        for (SkillNodeWrapper to : skills) {
            Map<ResourceLocation, Integer> prereqs = to.getRequirements();
            for (var entry : prereqs.entrySet()) {
                ResourceLocation fromId = entry.getKey();
                int reqLevel = entry.getValue();
                SkillNodeWrapper from = visibleMap.get(fromId);

                if (from != null) {
                    int fromX = startX + from.getGridX() * (CELL_SIZE + CELL_SPACING) + CELL_SIZE / 2;
                    int fromY = startY + from.getGridY() * (CELL_SIZE + CELL_SPACING) + CELL_SIZE / 2;
                    int toX = startX + to.getGridX() * (CELL_SIZE + CELL_SPACING) + CELL_SIZE / 2;
                    int toY = startY + to.getGridY() * (CELL_SIZE + CELL_SPACING) + CELL_SIZE / 2;

                    int fromBase = skillManager.getSkillLevel(fromId);
                    int fromPending = pendingUpgrades.getOrDefault(fromId, 0);
                    boolean satisfied = (fromBase + fromPending) >= reqLevel;

                    int color = satisfied ? 0xFF00AA00 : 0xFF553333;
                    drawConnector(g, fromX, fromY, toX, toY, color);
                }
            }
        }

        // Nodes
        for (SkillNodeWrapper wrapper : skills) {
            ResourceLocation skillId = wrapper.getSkillId();
            int x = startX + wrapper.getGridX() * (CELL_SIZE + CELL_SPACING);
            int y = startY + wrapper.getGridY() * (CELL_SIZE + CELL_SPACING);

            int baseLevel = skillManager.getSkillLevel(skillId);
            int pending = pendingUpgrades.getOrDefault(skillId, 0);
            int totalLevel = baseLevel + pending;
            int maxLevel = wrapper.getDefinition().getMaxLevel();

            boolean requirementsMet = checkRequirements(skillManager, wrapper, pendingUpgrades);
            boolean isMaxed = totalLevel >= maxLevel;
            boolean isHovered = mx >= x && mx < x + CELL_SIZE && my >= y && my < y + CELL_SIZE;

            int bgColor = isHovered ? 0xFF454545 : 0xFF303030;
            g.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, bgColor);
            g.renderOutline(x, y, CELL_SIZE, CELL_SIZE, GuiConstants.COLOR_PANEL_BORDER);

            // Icon
            ResourceLocation texture = SkillIconResolver.resolveSkillTexture(wrapper.getDefinition());
            RenderSystem.enableBlend();

            if (!requirementsMet)
                RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 1.0f);
            else
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            drawSkillIconLocal(g, x, y, texture, SkillIconResolver.getFallbackLabel(wrapper.getDefinition()),
                    requirementsMet);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            // Overlays
            if (!requirementsMet) {
                g.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, GuiConstants.COLOR_LOCKED);
            } else if (isMaxed) {
                g.renderOutline(x, y, CELL_SIZE, CELL_SIZE, GuiConstants.COLOR_MAXED);
                g.renderOutline(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2, GuiConstants.COLOR_MAXED);
            } else if (pending > 0) {
                g.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, GuiConstants.COLOR_LEARNED);
            }

            // Level badge
            String levelText = totalLevel + "/" + maxLevel;
            int textColor = pending > 0 ? 0xFFFFAA00 : (isMaxed ? 0xFFFFD700 : 0xFFFFFFFF);

            int badgeX = x + CELL_SIZE - 14;
            int badgeY = y + CELL_SIZE - 9;
            g.fill(badgeX, badgeY, badgeX + 14, badgeY + 9, 0xCC000000);

            g.pose().pushPose();
            g.pose().translate(badgeX + 7, badgeY + 5, 0);
            g.pose().scale(0.5f, 0.5f, 1.0f);
            int tw = this.font.width(levelText);
            g.drawString(this.font, levelText, -tw / 2, -4, textColor, false);
            g.pose().popPose();

            RenderSystem.disableBlend();
        }
    }

    private void drawConnector(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int thickness = 2;

        int midX = x2;
        int midY = y1;

        int hx1 = Math.min(x1, midX);
        int hx2 = Math.max(x1, midX);
        g.fill(hx1, y1 - thickness / 2, hx2, y1 + thickness / 2, color);

        int vy1 = Math.min(midY, y2);
        int vy2 = Math.max(midY, y2);
        g.fill(midX - thickness / 2, vy1, midX + thickness / 2, vy2, color);
    }

    private void renderFooterInfoLocal(GuiGraphics g, com.etema.ragnarmmo.common.api.stats.IPlayerStats stats) {
        int originalPoints = stats.getSkillPoints();
        int spentPoints = pendingUpgrades.values().stream().mapToInt(Integer::intValue).sum();
        int currentPoints = originalPoints - spentPoints;

        int spX = 8;
        int spY = PANEL_HEIGHT - 24;

        int spColor = currentPoints > 0 ? 0xFF00FF00 : (spentPoints > 0 ? 0xFFFFAA00 : 0xFF888888);

        // Stat Points: 0 Skill Points: 5
        String skillPointsLabel = Component.translatable("screen.ragnarmmo.stats.points", 0, currentPoints).getString();
        if (skillPointsLabel.contains("  ")) {
            skillPointsLabel = skillPointsLabel.substring(skillPointsLabel.indexOf("  ") + 2);
        }
        g.drawString(this.font, skillPointsLabel, spX, spY, spColor, true);

        if (spentPoints > 0) {
            Component pendingText = Component.translatable("screen.ragnarmmo.skills.points_spent", spentPoints);
            g.drawString(this.font, pendingText, spX + this.font.width(skillPointsLabel), spY, 0xFFFFAA00, false);
        }
    }

    private List<Component> buildSkillTooltipLocal(com.etema.ragnarmmo.skill.runtime.SkillManager skillManager,
            int startX, int startY, double mx, double my) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return null;

        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty())
            return null;

        var stats = statsOpt.get();
        int originalPoints = stats.getSkillPoints();
        int spentPoints = pendingUpgrades.values().stream().mapToInt(Integer::intValue).sum();
        int currentPoints = originalPoints - spentPoints;

        for (SkillNodeWrapper wrapper : visibleSkills) {
            int x = startX + wrapper.getGridX() * (CELL_SIZE + CELL_SPACING);
            int y = startY + wrapper.getGridY() * (CELL_SIZE + CELL_SPACING);

            if (mx >= x && mx < x + CELL_SIZE && my >= y && my < y + CELL_SIZE) {
                List<Component> tooltipLines = new ArrayList<>();

                ResourceLocation skillId = wrapper.getSkillId();
                int baseLevel = skillManager.getSkillLevel(skillId);
                int pending = pendingUpgrades.getOrDefault(skillId, 0);
                int totalLevel = baseLevel + pending;
                int maxLevel = wrapper.getDefinition().getMaxLevel();

                boolean requirementsMet = checkRequirements(skillManager, wrapper, pendingUpgrades);
                boolean isMaxed = totalLevel >= maxLevel;

                tooltipLines.add(Component.translatable(wrapper.getDefinition().getTranslationKey())
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

                net.minecraft.network.chat.MutableComponent lvlLine;
                if (pending > 0) {
                    lvlLine = Component.translatable("screen.ragnarmmo.skills.tooltip.level_upgrade", baseLevel,
                            totalLevel, maxLevel);
                } else {
                    lvlLine = Component.translatable("screen.ragnarmmo.skills.tooltip.level", baseLevel, maxLevel);
                }
                tooltipLines.add(lvlLine.withStyle(ChatFormatting.WHITE));

                boolean canSpendPoints = wrapper.getDefinition().canUpgradeWithPoints();
                if (canSpendPoints) {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.cost",
                            wrapper.getDefinition().getUpgradeCost())
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.auto_unlock")
                            .withStyle(ChatFormatting.GRAY));
                }
                tooltipLines
                        .add(Component.translatable("screen.ragnarmmo.skills.tooltip.primary",
                                wrapper.getDefinition().getScalingStat())
                                .withStyle(ChatFormatting.GRAY));

                tooltipLines.add(Component.literal(""));
                String descKey = wrapper.getDefinition().getTranslationKey() + ".desc";
                if (net.minecraft.client.resources.language.I18n.exists(descKey)) {
                    String descText = net.minecraft.client.resources.language.I18n.get(descKey);
                    for (String line : descText.split("\n")) {
                        tooltipLines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                    }
                }

                if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
                    tooltipLines.add(Component.literal(""));
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.advanced").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                    
                    int displayLevel = Math.max(1, totalLevel);
                    tooltipLines.add(Component.literal(" SP Cost: " + wrapper.getDefinition().getResourceCost(displayLevel)).withStyle(ChatFormatting.DARK_GRAY));
                    tooltipLines.add(Component.literal(" Cast Time: " + (wrapper.getDefinition().getCastTimeTicks(displayLevel) / 20.0f) + "s").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    tooltipLines.add(Component.literal(""));
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.hold_ctrl").withStyle(ChatFormatting.DARK_GRAY));
                }

                var requirements = wrapper.getRequirements();
                if (!requirements.isEmpty()) {
                    tooltipLines.add(Component.literal(""));
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.requires")
                            .withStyle(ChatFormatting.GOLD));
                    for (var req : requirements.entrySet()) {
                        ResourceLocation reqId = req.getKey();
                        int reqLevel = req.getValue();
                        int currentBase = skillManager.getSkillLevel(reqId);
                        int currentPending = pendingUpgrades.getOrDefault(reqId, 0);
                        boolean met = (currentBase + currentPending) >= reqLevel;

                        var style = met ? ChatFormatting.GREEN : ChatFormatting.RED;
                        // Get translation key for the required skill
                        String reqTransKey = "skill.ragnarmmo." + reqId.getPath();
                        tooltipLines.add(Component.literal("  - ")
                                .append(Component.translatable(reqTransKey))
                                .append(Component.literal(" Lv." + reqLevel))
                                .withStyle(style));
                    }
                }

                tooltipLines.add(Component.literal(""));
                if (!requirementsMet) {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.locked")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                } else if (!canSpendPoints) {
                    if (baseLevel > 0) {
                        tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.unlocked")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    } else {
                        tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.auto_unlock")
                                .withStyle(ChatFormatting.YELLOW));
                    }
                } else if (isMaxed) {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.maxed")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                } else if (pending > 0) {
                    tooltipLines
                            .add(Component.translatable("screen.ragnarmmo.skills.tooltip.pending", pending)
                                    .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.available")
                            .withStyle(ChatFormatting.GREEN));
                }

                boolean canUpgrade = canSpendPoints && totalLevel < maxLevel && currentPoints > 0 && requirementsMet;
                if (canUpgrade)
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.left_click")
                            .withStyle(ChatFormatting.GREEN));
                if (pending > 0)
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.right_click")
                            .withStyle(ChatFormatting.YELLOW));

                return tooltipLines;
            }
        }
        return null;
    }

    // === LIFE SKILLS RENDERING ===

    private void renderLifeSkillTreeLocal(GuiGraphics g,
            com.etema.ragnarmmo.system.lifeskills.LifeSkillManager lifeManager,
            int startX, int startY, double mx, double my) {

        int col = 0;
        int row = 0;
        int maxCols = 3;

        
        // Simpler calculation for Life Skills since it's a grid
        int totalRows = (visibleLifeSkills.size() + maxCols - 1) / maxCols;
        int totalHeight = startY + totalRows * (CELL_SIZE + CELL_SPACING + 8);
        maxScroll = Math.max(0, totalHeight + 10 - (PANEL_HEIGHT - 65));

        col = 0;
        row = 0;
        for (LifeSkillType type : visibleLifeSkills) {
            int x = startX + col * (CELL_SIZE + CELL_SPACING + 48);
            int y = startY + row * (CELL_SIZE + CELL_SPACING + 8);

            LifeSkillProgress progress = lifeManager.getSkill(type);
            int level = progress != null ? progress.getLevel() : 0;
            int points = progress != null ? progress.getPoints() : 0;
            int pointsRequired = progress != null ? progress.getPointsToNextLevel() : 100;
            int maxLevel = 100;

            boolean isHovered = mx >= x && mx < x + CELL_SIZE + 48 && my >= y && my < y + CELL_SIZE;
            boolean isMaxed = level >= maxLevel;

            int bgColor = isHovered ? 0xFF454545 : 0xFF303030;
            g.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, bgColor);
            g.renderOutline(x, y, CELL_SIZE, CELL_SIZE, GuiConstants.COLOR_PANEL_BORDER);

            ResourceLocation texture = SkillIconResolver.resolveLifeSkillTexture(type);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            drawSkillIconLocal(g, x, y, texture, SkillIconResolver.getFallbackLabel(type), true);

            if (isMaxed) {
                g.renderOutline(x, y, CELL_SIZE, CELL_SIZE, GuiConstants.COLOR_MAXED);
                g.renderOutline(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2, GuiConstants.COLOR_MAXED);
            }

            // Level badge
            String levelText = String.valueOf(level);
            int textColor = isMaxed ? 0xFFFFD700 : 0xFFFFFFFF;

            int badgeX = x + CELL_SIZE - 10;
            int badgeY = y + CELL_SIZE - 9;
            g.fill(badgeX - 2, badgeY, badgeX + 10, badgeY + 9, 0xCC000000);
            g.drawString(this.font, levelText, badgeX, badgeY + 1, textColor, false);

            // Name + bar (right)
            int barX = x + CELL_SIZE + 4;
            int barY = y + 4;
            int barWidth = 44;
            int barHeight = 6;

            g.drawString(this.font, Component.translatable(type.getTranslationKey()), barX, barY, 0xFFCCCCCC, false);

            int progBarY = barY + 12;
            g.fill(barX, progBarY, barX + barWidth, progBarY + barHeight, 0xFF222222);

            if (!isMaxed && pointsRequired > 0) {
                float ratio = Math.min(1.0f, (float) points / (float) pointsRequired);
                int fillWidth = (int) (barWidth * ratio);
                g.fill(barX, progBarY, barX + fillWidth, progBarY + barHeight, 0xFF00AA00);
            } else if (isMaxed) {
                g.fill(barX, progBarY, barX + barWidth, progBarY + barHeight, 0xFFFFD700);
            }

            g.renderOutline(barX, progBarY, barWidth, barHeight, 0xFF666666);

            String pointsText = isMaxed ? Component.translatable("screen.ragnarmmo.skills.tooltip.maxed").getString()
                    : points + "/" + pointsRequired;
            if (pointsText.startsWith("* "))
                pointsText = pointsText.substring(2); // Remove the asterisk if present in maxed key

            g.pose().pushPose();
            g.pose().translate(barX, progBarY + barHeight + 2, 0);
            g.pose().scale(0.5f, 0.5f, 1.0f);
            g.drawString(this.font, pointsText, 0, 0, 0xFF888888, false);
            g.pose().popPose();

            RenderSystem.disableBlend();

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
    }

    private void renderLifeFooterInfoLocal(GuiGraphics g,
            com.etema.ragnarmmo.system.lifeskills.LifeSkillManager lifeManager) {
        double avg = Arrays.stream(LifeSkillType.values())
                .map(lifeManager::getSkill)
                .filter(Objects::nonNull)
                .mapToInt(LifeSkillProgress::getLevel)
                .average()
                .orElse(0.0);

        int baseY = Math.max(8, FOOTER_Y - 22);

        // Keep the footer inside the content scissor so the first glyph doesn't get clipped.
        int spX = 16;
        int spY = baseY;

        String avgStr = String.format("%.1f", avg);
        Component infoText = Component.translatable("screen.ragnarmmo.skills.life.total", avgStr);
        g.drawString(this.font, infoText, spX, spY, 0xFF88FF88, true);

        Component hintText = Component.translatable("screen.ragnarmmo.skills.life.hint");
        g.pose().pushPose();
        g.pose().translate(spX, spY + 10, 0);
        g.pose().scale(0.75f, 0.75f, 1.0f);
        g.drawString(this.font, hintText, 0, 0, 0xFF666666, false);
        g.pose().popPose();
    }

    private List<Component> buildLifeTooltipLocal(com.etema.ragnarmmo.system.lifeskills.LifeSkillManager lifeManager,
            int startX, int startY, double mx, double my) {

        int col = 0;
        int row = 0;
        int maxCols = 3;

        for (LifeSkillType type : visibleLifeSkills) {
            int x = startX + col * (CELL_SIZE + CELL_SPACING + 48);
            int y = startY + row * (CELL_SIZE + CELL_SPACING + 8);

            if (mx >= x && mx < x + CELL_SIZE + 48 && my >= y && my < y + CELL_SIZE) {
                LifeSkillProgress progress = lifeManager.getSkill(type);
                int level = progress != null ? progress.getLevel() : 0;
                int points = progress != null ? progress.getPoints() : 0;
                int pointsRequired = progress != null ? progress.getPointsToNextLevel() : 100;
                int maxLevel = 100;
                boolean isMaxed = level >= maxLevel;

                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(Component.translatable(type.getTranslationKey())
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

                tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.life.type")
                        .withStyle(ChatFormatting.DARK_AQUA));
                tooltipLines
                        .add(Component.translatable("screen.ragnarmmo.skills.tooltip.level", level, maxLevel)
                                .withStyle(ChatFormatting.WHITE));

                if (!isMaxed) {
                    tooltipLines
                            .add(Component.translatable("screen.ragnarmmo.skills.life.progress", points, pointsRequired)
                                    .withStyle(ChatFormatting.GREEN));
                }

                tooltipLines.add(Component.literal(""));
                String descKey = type.getTranslationKey().replace("lifeskill", "skill") + ".desc";
                if (net.minecraft.client.resources.language.I18n.exists(descKey)) {
                    String descText = net.minecraft.client.resources.language.I18n.get(descKey);
                    for (String line : descText.split("\n")) {
                        tooltipLines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                    }
                }

                tooltipLines.add(Component.literal(""));
                if (isMaxed) {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.maxed")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                } else {
                    tooltipLines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.available")
                            .withStyle(ChatFormatting.GREEN));
                }

                tooltipLines.add(Component.literal(""));
                tooltipLines.add(Component
                        .translatable("screen.ragnarmmo.skills.life.activity",
                                Component.translatable("skill.ragnarmmo.activity." + type.getId()))
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

                return tooltipLines;
            }

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Convert mouse to LOCAL
        double mx = (mouseX - panelX) / uiScale;
        double my = (mouseY - panelY) / uiScale;
        
        // Buttons and tabs are NOT scrolled
        // We only apply scroll to skill interaction
        double scrollMy = my + scrollOffset;

        var player = Minecraft.getInstance().player;
        boolean showApplyReset = isRegularTab() && player != null;
        layoutFooterButtons(showApplyReset);

        // Manual buttons
        if (backRect != null && backRect.contains(mx, my)) {
            if (this.minecraft != null)
                this.minecraft.setScreen(parent);
            playClickSound(1.0f);
            return true;
        }

        if (BTN_CHANGE_CLASS.contains(mx, my) && player != null) {
            boolean canChangeClass = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player)
                    .map(stats -> JobType.fromId(stats.getJobId()).hasPromotions())
                    .orElse(false);
            if (canChangeClass) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new JobSelectionScreen(this));
                }
                playClickSound(1.0f);
                return true;
            }
        }

        if (showApplyReset) {
            if (resetRect != null && resetRect.contains(mx, my) && resetEnabled()) {
                pendingUpgrades.clear();
                playClickSound(0.9f);
                return true;
            }
            if (applyRect != null && applyRect.contains(mx, my) && applyEnabled(player)) {
                applyChanges();
                playClickSound(1.0f);
                return true;
            }
        }

        // Tabs
        if (handleTabClickLocal(mx, my)) {
            return true;
        }

        // Skills (WITH scroll)
        return handleSkillClickLocal(mx, scrollMy, button);
    }

    private boolean handleTabClickLocal(double mx, double my) {
        int tabY = -28;
        int tabWidth = 65;

        for (int i = 0; i < activeTabs.size(); i++) {
            int tx = 10 + (i * 70);
            if (mx >= tx && mx < tx + tabWidth && my >= tabY && my < tabY + 28) {
                if (activeTabIndex != i) {
                    activeTabIndex = i;
                    updateVisibleSkills();
                    scrollOffset = 0;

                    if (getActiveTab().isLifeTab()) {
                        pendingUpgrades.clear();
                    }

                    playClickSound(1.0f);
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSkillClickLocal(double mx, double my, int button) {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return false;

        if (getActiveTab().isLifeTab()) {
            return false; // life skills are point-based; no click upgrades
        }

        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty())
            return false;

        var stats = statsOpt.get();
        int currentPoints = stats.getSkillPoints()
                - pendingUpgrades.values().stream().mapToInt(Integer::intValue).sum();

        int startX = 16;
        int startY = 54;

        for (SkillNodeWrapper wrapper : visibleSkills) {
            int x = startX + wrapper.getGridX() * (CELL_SIZE + CELL_SPACING);
            int y = startY + wrapper.getGridY() * (CELL_SIZE + CELL_SPACING);

            if (mx >= x && mx < x + CELL_SIZE && my >= y && my < y + CELL_SIZE) {
                PlayerSkillsProvider.get(player).ifPresent(skillManager -> {
                    ResourceLocation skillId = wrapper.getSkillId();
                    int baseLevel = skillManager.getSkillLevel(skillId);
                    int pending = pendingUpgrades.getOrDefault(skillId, 0);
                    int totalLevel = baseLevel + pending;
                    int maxLevel = wrapper.getDefinition().getMaxLevel();

                    boolean canSpendPoints = wrapper.getDefinition().canUpgradeWithPoints();

                    if (button == 0) { // Left click upgrade
                        if (canSpendPoints && totalLevel < maxLevel && currentPoints > 0
                                && checkRequirements(skillManager, wrapper, pendingUpgrades)) {
                            pendingUpgrades.put(skillId, pending + 1);
                            playClickSound(1.0f);
                        }
                    } else if (button == 1) { // Right click undo
                        if (pending > 0) {
                            if (pending == 1)
                                pendingUpgrades.remove(skillId);
                            else
                                pendingUpgrades.put(skillId, pending - 1);
                            playClickSound(0.9f);
                        }
                    }
                });
                return true;
            }
        }

        return false;
    }

    private void playClickSound(float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private void drawSkillIconLocal(GuiGraphics g, int cellX, int cellY, ResourceLocation texture,
            String fallbackLabel, boolean enabled) {
        int iconOffset = (CELL_SIZE - ICON_SIZE) / 2;
        int iconX = cellX + iconOffset;
        int iconY = cellY + iconOffset;

        if (texture != null) {
            float scale = ICON_SIZE / 64.0f;
            g.pose().pushPose();
            g.pose().translate(iconX, iconY, 0);
            g.pose().scale(scale, scale, 1.0f);
            g.blit(texture, 0, 0, 0, 0, 64, 64, 64, 64);
            g.pose().popPose();
            return;
        }

        int bg = enabled ? 0xFF1F1F1F : 0xFF171717;
        int border = enabled ? 0xFF777777 : 0xFF4D4D4D;
        int textColor = enabled ? 0xFFE6E6E6 : 0xFF8A8A8A;
        g.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, bg);
        g.renderOutline(iconX, iconY, ICON_SIZE, ICON_SIZE, border);

        String label = fallbackLabel == null || fallbackLabel.isBlank() ? "?" : fallbackLabel;
        g.pose().pushPose();
        g.pose().translate(iconX + ICON_SIZE / 2.0f, iconY + ICON_SIZE / 2.0f, 0);
        g.pose().scale(0.75f, 0.75f, 1.0f);
        int textWidth = this.font.width(label);
        g.drawString(this.font, label, -textWidth / 2, -4, textColor, false);
        g.pose().popPose();
    }

    private boolean checkRequirements(com.etema.ragnarmmo.skill.runtime.SkillManager manager, SkillNodeWrapper wrapper,
            Map<ResourceLocation, Integer> pending) {
        for (var entry : wrapper.getRequirements().entrySet()) {
            ResourceLocation reqId = entry.getKey();
            int reqLevel = entry.getValue();
            int currentBase = manager.getSkillLevel(reqId);
            int currentPending = pending.getOrDefault(reqId, 0);
            if ((currentBase + currentPending) < reqLevel)
                return false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC -> back
        if (com.etema.ragnarmmo.client.Keybinds.OPEN_SKILLS.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            return true;
        }

        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            if (this.minecraft != null)
                this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (float) delta * 20));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
