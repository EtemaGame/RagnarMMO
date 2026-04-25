package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.hud.HudRenderUtil;
import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
import com.etema.ragnarmmo.client.hud.HudWidgetState;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.player.progression.PlayerProgressionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * MMO Classic (Ragnarok Online) style HUD overlay.
 * Single integrated panel centered above the vanilla hotbar.
 */
public class RagnarStatusOverlay implements IGuiOverlay {

    public static final RagnarStatusOverlay INSTANCE = new RagnarStatusOverlay();

    // Layout constants
    private static final int PANEL_PADDING = 5;
    private static final int BAR_HEIGHT = 8;
    private static final int XP_BAR_HEIGHT = 4;
    private static final int LINE_SPACING = 2;
    private static final int LABEL_WIDTH = 20; // width reserved for "HP", "SP" labels
    private static final int VALUE_GAP = 4; // gap between bar and value text

    public static final int HANDLE_SIZE = 8;

    private RagnarStatusOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get())
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator())
            return;

        RagnarCoreAPI.get(player).ifPresent(stats -> {
            Font font = mc.font;

            // Status panel (Name + HP + SP + EXP — full integrated panel)
            renderComponent(graphics, font, RagnarConfigs.CLIENT.hud.status,
                    screenWidth, screenHeight,
                    (g, f, w) -> renderStatus(g, f, stats, player, w),
                    f -> computePanelHeight(f));
        });
    }

    /**
     * Renders a single HUD component at its configured screen position.
     */
    private static void renderComponent(GuiGraphics graphics, Font font,
            RagnarConfigs.Client.Hud.HudComponent config,
            int screenWidth, int screenHeight,
            ComponentRenderer renderer,
            java.util.function.ToIntFunction<Font> heightProvider) {

        HudWidgetState state = HudConfigSerializer.read(config);
        if (!state.enabled())
            return;

        int panelW = Mth.clamp(RagnarConfigs.CLIENT.hud.width.get(), 120, 400);
        int compH = heightProvider.applyAsInt(font);
        HudLayoutManager.HudBounds bounds = HudLayoutManager.bounds(state, panelW, compH, screenWidth, screenHeight);

        // Optional background
        HudLayoutManager.renderBackground(graphics, state, bounds);

        HudLayoutManager.pushWidgetTransform(graphics, bounds);
        renderer.render(graphics, font, bounds.width());

        HudLayoutManager.popWidgetTransform(graphics);
    }

    @FunctionalInterface
    private interface ComponentRenderer {
        int render(GuiGraphics g, Font f, int width);
    }

    private static int computePanelHeight(Font font) {
        int lh = font.lineHeight;
        // name row + HP bar + SP bar + Food bar + XP bars + padding/spacing
        return (PANEL_PADDING + 2) * 2 // top + bottom padding
                + lh // name row
                + LINE_SPACING
                + BAR_HEIGHT // HP bar
                + LINE_SPACING
                + BAR_HEIGHT // SP bar
                + LINE_SPACING
                + BAR_HEIGHT // Food bar
                + LINE_SPACING + 1
                + lh + XP_BAR_HEIGHT; // XP label + XP bar
    }

    // ── Row 1: Name + Levels ──

    private static int renderNameRow(GuiGraphics graphics, Font font, IPlayerStats stats,
            Player player, int x, int y, int w) {
        // Player name on the left
        String name = player.getName().getString();
        HudRenderUtil.drawTextShadow(graphics, font, name, x, y, 0xFFFFFFFF);

        // "Lv.X Job.X" on the right
        String levels = "Lv." + stats.getLevel() + "  Job." + stats.getJobLevel();
        HudRenderUtil.drawTextShadowRight(graphics, font, levels, x + w, y, GuiConstants.COLOR_TEXT_DIM);

        return y + font.lineHeight;
    }

    // ── Row 2: HP Bar ──

    private static int renderHpBar(GuiGraphics graphics, Font font, Player player,
            int x, int y, int w) {
        double hp = player.getHealth();
        double maxHp = player.getMaxHealth();
        float hpPct = maxHp > 0 ? (float) (hp / maxHp) : 0f;

        // "HP" label
        HudRenderUtil.drawTextShadow(graphics, font, "HP", x, y - 1, GuiConstants.COLOR_HP_BAR);

        // Value text "123/456"
        String val = (int) hp + "/" + (int) maxHp;
        int valW = font.width(val);

        // Bar fills the space between label and value
        int barX = x + LABEL_WIDTH;
        int barW = w - LABEL_WIDTH - valW - VALUE_GAP;
        HudRenderUtil.drawGradientBar(graphics, barX, y, barW, BAR_HEIGHT, hpPct,
                GuiConstants.COLOR_HP_BAR, GuiConstants.COLOR_HP_BAR_DARK);

        // Value right of bar
        HudRenderUtil.drawTextShadow(graphics, font, val,
                barX + barW + VALUE_GAP, y - 1, 0xFFFFFFFF);

        return y + BAR_HEIGHT;
    }

    // ── Row 3: SP/Mana Bar ──

    private static int renderSpBar(GuiGraphics graphics, Font font, IPlayerStats stats,
            int x, int y, int w) {
        if (!(stats instanceof com.etema.ragnarmmo.player.stats.capability.PlayerStats ps)) {
            return y + BAR_HEIGHT;
        }

        var job = JobType.fromId(stats.getJobId());
        double current = ps.getCurrentResource();
        double max = ps.getMaxResource();
        float pct = max > 0 ? (float) (current / max) : 0f;
        String label = job.getResourceLabel();
        boolean magical = job.isMagical();

        int colorTop = magical ? GuiConstants.COLOR_SP_BAR : GuiConstants.COLOR_SP_BAR_PHYS;
        int colorBot = magical ? GuiConstants.COLOR_SP_BAR_DARK : GuiConstants.COLOR_SP_BAR_PHYS_DARK;

        // Label "SP" or "MP"
        HudRenderUtil.drawTextShadow(graphics, font, label, x, y - 1, colorTop);

        // Value text
        String val = (int) current + "/" + (int) max;
        int valW = font.width(val);

        // Bar
        int barX = x + LABEL_WIDTH;
        int barW = w - LABEL_WIDTH - valW - VALUE_GAP;
        HudRenderUtil.drawGradientBar(graphics, barX, y, barW, BAR_HEIGHT, pct,
                colorTop, colorBot);

        // Value right of bar
        HudRenderUtil.drawTextShadow(graphics, font, val,
                barX + barW + VALUE_GAP, y - 1, 0xFFFFFFFF);

        return y + BAR_HEIGHT;
    }

    // ── Row 4: Food Bar ──

    private static int renderFoodBar(GuiGraphics graphics, Font font, Player player,
            int x, int y, int w) {
        int food = player.getFoodData().getFoodLevel();
        float pct = food / 20f;

        // "FD" label
        HudRenderUtil.drawTextShadow(graphics, font, "FD", x, y - 1, 0xFFFFAA00);

        // Value text
        String val = food + "/20";
        int valW = font.width(val);

        // Bar
        int barX = x + LABEL_WIDTH;
        int barW = w - LABEL_WIDTH - valW - VALUE_GAP;
        HudRenderUtil.drawGradientBar(graphics, barX, y, barW, BAR_HEIGHT, pct,
                0xFFFFAA00, 0xFF906000);

        // Value right of bar
        HudRenderUtil.drawTextShadow(graphics, font, val,
                barX + barW + VALUE_GAP, y - 1, 0xFFFFFFFF);

        return y + BAR_HEIGHT;
    }

    // ── Row 5: EXP Bars (Base + Job) ──

    private static void renderExpBars(GuiGraphics graphics, Font font, IPlayerStats stats,
            int x, int y, int w) {
        PlayerProgressionService progressionService = PlayerProgressionService
                .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()));
        float basePct = progressionService.getBaseProgressPercent(stats.getExp(), stats.getLevel());
        float jobPct = progressionService.getJobProgressPercent(stats.getJobExp(), stats.getJobLevel());

        // Split into two halves with a small gap
        int gap = 6;
        int halfW = (w - gap) / 2;

        // Base EXP label + bar
        String baseLabel = "EXP";
        int baseLabelW = font.width(baseLabel) + 3;
        HudRenderUtil.drawTextShadow(graphics, font, baseLabel, x, y, GuiConstants.COLOR_XP_BAR);
        HudRenderUtil.drawThinBar(graphics, x + baseLabelW, y + font.lineHeight - XP_BAR_HEIGHT,
                halfW - baseLabelW, XP_BAR_HEIGHT, basePct,
                GuiConstants.COLOR_XP_BAR, GuiConstants.COLOR_XP_BAR_DARK);

        // Job EXP label + bar
        int jobX = x + halfW + gap;
        String jobLabel = "JOB";
        int jobLabelW = font.width(jobLabel) + 3;
        HudRenderUtil.drawTextShadow(graphics, font, jobLabel, jobX, y, GuiConstants.COLOR_JOB_XP_BAR);
        HudRenderUtil.drawThinBar(graphics, jobX + jobLabelW, y + font.lineHeight - XP_BAR_HEIGHT,
                halfW - jobLabelW, XP_BAR_HEIGHT, jobPct,
                GuiConstants.COLOR_JOB_XP_BAR, GuiConstants.COLOR_JOB_XP_BAR_DARK);
    }

    // ── Public static renderers (used by HudOverlayConfigScreen for previews) ──

    public static int renderStatus(GuiGraphics graphics, Font font, IPlayerStats stats, Player player, int width) {
        int panelH = computePanelHeight(font);
        HudRenderUtil.drawPanel(graphics, 0, 0, width, panelH);
        int contentX = PANEL_PADDING + 2;
        int contentW = width - (PANEL_PADDING + 2) * 2;
        int cy = PANEL_PADDING + 2;
        cy = renderNameRow(graphics, font, stats, player, contentX, cy, contentW);
        cy += LINE_SPACING;
        cy = renderHpBar(graphics, font, player, contentX, cy, contentW);
        cy += LINE_SPACING;
        cy = renderSpBar(graphics, font, stats, contentX, cy, contentW);
        cy += LINE_SPACING;
        cy = renderFoodBar(graphics, font, player, contentX, cy, contentW);
        cy += LINE_SPACING + 1;
        renderExpBars(graphics, font, stats, contentX, cy, contentW);
        return panelH;
    }

    public static int renderHp(GuiGraphics graphics, Font font, Player player, int width) {
        renderHpBar(graphics, font, player, 2, 2, width - 4);
        return BAR_HEIGHT + 4;
    }

    public static int renderSp(GuiGraphics graphics, Font font, IPlayerStats stats, int width) {
        renderSpBar(graphics, font, stats, 2, 2, width - 4);
        return BAR_HEIGHT + 4;
    }

    public static int renderFood(GuiGraphics graphics, Font font, Player player, int width) {
        // Food is no longer a separate component in CLASSIC mode, render minimal
        // preview
        int food = player.getFoodData().getFoodLevel();
        float pct = food / 20f;
        HudRenderUtil.drawGradientBar(graphics, 2, 2, width - 4, 6, pct,
                0xFFFFAA00, 0xFF906000);
        String val = "Food " + food + "/20";
        int valW = font.width(val);
        HudRenderUtil.drawTextShadow(graphics, font, val, (width - valW) / 2, 2, 0xFFFFFF);
        return font.lineHeight + 8;
    }

    // ── Dimension helpers (used by HudOverlayConfigScreen) ──

    public static int getStatusHeight(Font font) {
        return computePanelHeight(font);
    }

    public static int getBarHeight(Font font) {
        return font.lineHeight + 2 + 6 + 3;
    }
}
