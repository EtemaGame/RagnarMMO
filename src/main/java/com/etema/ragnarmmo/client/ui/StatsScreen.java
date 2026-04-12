package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.WeightConstants;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.skill.job.merchant.MerchantSkillEvents;
import com.etema.ragnarmmo.client.DerivedStatsClientCache;
import com.etema.ragnarmmo.system.stats.compute.StatComputer;
import com.etema.ragnarmmo.system.stats.net.AllocateStatPacket;
import com.etema.ragnarmmo.system.stats.net.DeallocateStatPacket;
import com.etema.ragnarmmo.system.stats.net.PacketResetCharacter;
import com.etema.ragnarmmo.system.stats.progression.ExpTable;
import com.etema.ragnarmmo.system.stats.progression.JobBonusService;
import com.etema.ragnarmmo.roitems.runtime.RoAttributeApplier;
import com.etema.ragnarmmo.system.stats.progression.StatCost;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.minecraftforge.items.ItemStackHandler;

import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class StatsScreen extends Screen {

        private static final StatKeys[] BASE_STATS = {
                        StatKeys.STR, StatKeys.AGI, StatKeys.VIT, StatKeys.INT, StatKeys.DEX, StatKeys.LUK
        };

        // === PANEL CONSTANTS (LOCAL COORDS) ===
        private static final int PANEL_WIDTH = 360;
        private static final int PANEL_HEIGHT = 320;

        private static final int HEADER_HEIGHT = 35;
        private static final int FOOTER_HEIGHT = 40;

        private static final int BTN_SIZE_SMALL = 14;

        private static final ResourceLocation TEX_GEAR = new ResourceLocation("ragnarmmo", "textures/gui/gear.png");
        private static final ResourceLocation ENLARGE_WEIGHT_LIMIT = new ResourceLocation("ragnarmmo",
                        "enlarge_weight_limit");
        private static final ResourceLocation PUSHCART = new ResourceLocation("ragnarmmo", "pushcart");

        // ===== Scaling placement =====
        private float uiScale = 1.0f;
        private int panelX = 0; // screen coords
        private int panelY = 0; // screen coords

        // ===== Hover tooltip deferred =====
        private java.util.List<Component> deferredTooltip = null;

        // ===== Clickable rects =====
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

        // Header buttons (local)
        private static final Rect BTN_GEAR = new Rect(PANEL_WIDTH - 18 - 8, 6, 18, 18);
        private static final Rect BTN_SKILLS = new Rect(PANEL_WIDTH - 18 - 8 - 4 - 50, 6, 50, 18);

        // Footer buttons (local)
        private static final Rect BTN_RESET_STATS = new Rect(PANEL_WIDTH - 90, PANEL_HEIGHT - FOOTER_HEIGHT + 10, 80,
                        18);
        // Moved to Header (Left of Skills)
        // BTN_SKILLS x is roughly PANEL_WIDTH - 80.
        // Let's place Change Class to the left of it.
        // BTN_SKILLS = new Rect(PANEL_WIDTH - 18 - 8 - 4 - 50, 6, 50, 18);
        // x = (PANEL_WIDTH - 80) - 4 - 80 = PANEL_WIDTH - 164 roughly
        private static final Rect BTN_CHANGE_CLASS = new Rect(PANEL_WIDTH - 18 - 8 - 4 - 50 - 4 - 80, 6, 80, 18);

        // Stat +/- rects computed per-frame (local)
        private final Map<StatKeys, Rect> plusRect = new EnumMap<>(StatKeys.class);
        private final Map<StatKeys, Rect> minusRect = new EnumMap<>(StatKeys.class);

        // Store the actual Y where each stat row (STR/AGI/...) is drawn
        private final Map<StatKeys, Integer> statRowY = new EnumMap<>(StatKeys.class);

        // =========================
        // Encumbrance UI (client-side display)
        // Uses WeightConstants for sync with server (MerchantSkillEvents)
        // =========================

        private static int uiGetWeightLimitLevel(Player player) {
                return PlayerSkillsProvider.get(player)
                                .map(sk -> sk.getSkillLevel(ENLARGE_WEIGHT_LIMIT))
                                .orElse(0);
        }

        private static int uiGetPushcartLevel(Player player) {
                return PlayerSkillsProvider.get(player)
                                .map(sk -> sk.getSkillLevel(PUSHCART))
                                .orElse(0);
        }

        private static ItemStackHandler uiGetCartInventory(Player player) {
                return PlayerSkillsProvider.get(player)
                                .map(sk -> sk.getCartInventory())
                                .orElse(null);
        }

        private static double uiComputeTotalWeight(Player player, int cartLevel) {
                double total = 0.0D;
                for (ItemStack s : player.getInventory().items) {
                        total += MerchantSkillEvents.computeWeight(s);
                }
                ItemStackHandler cartInv = uiGetCartInventory(player);
                if (cartInv != null) {
                        double cartWeight = 0.0D;
                        for (int i = 0; i < cartInv.getSlots(); i++) {
                                cartWeight += MerchantSkillEvents.computeWeight(cartInv.getStackInSlot(i));
                        }
                        double red = MerchantSkillEvents.cartWeightReduction(cartLevel);
                        total += cartWeight * (1.0D - red);
                }
                return total;
        }

        private static double uiComputeCapacity(IPlayerStats stats, int cartLevel) {
                int str = stats.getSTR();
                return WeightConstants.BASE_CAPACITY
                                + (str * WeightConstants.STR_CAPACITY_PER_POINT)
                                + MerchantSkillEvents.capacityBonusFromEnlargeWeightLimit(cartLevel);
        }

        public StatsScreen() {
                super(Component.translatable("screen.ragnarmmo.title"));
        }

        @Override
        protected void init() {
                recalcPanelTransform();
        }

        @Override
        public void resize(Minecraft mc, int width, int height) {
                super.resize(mc, width, height);
                recalcPanelTransform();
        }

        private void recalcPanelTransform() {
                float sx = (float) this.width / (float) PANEL_WIDTH;
                float sy = (float) this.height / (float) PANEL_HEIGHT;
                this.uiScale = Math.min(1.0f, Math.min(sx, sy));

                int scaledW = Math.round(PANEL_WIDTH * uiScale);
                int scaledH = Math.round(PANEL_HEIGHT * uiScale);

                this.panelX = (this.width - scaledW) / 2;
                this.panelY = (this.height - scaledH) / 2;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
                var mc = Minecraft.getInstance();
                var player = mc.player;

                this.renderBackground(g);
                this.deferredTooltip = null;

                // Map mouse to LOCAL coords
                double mx = (mouseX - panelX) / uiScale;
                double my = (mouseY - panelY) / uiScale;

                // Draw scaled panel
                g.pose().pushPose();
                g.pose().translate(panelX, panelY, 0);
                g.pose().scale(uiScale, uiScale, 1.0f);

                // Panel background
                g.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BG);
                g.renderOutline(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BORDER);

                // Header title
                g.drawString(mc.font, Component.translatable("screen.ragnarmmo.title"), 16, 12, 0xFFFFAA00, true);

                if (player != null) {
                        RagnarCoreAPI.get(player).ifPresent(stats -> {

                                // Header info line
                                Component levelText = Component.translatable("screen.ragnarmmo.stats.level_format",
                                                stats.getLevel(), stats.getExp(), ExpTable.expToNext(stats.getLevel()));
                                g.drawString(mc.font, levelText, 19, 28, 0xFFFFFFFF, false);

                                // =========================
                                // VISUAL RHYTHM CONSTANTS
                                // =========================
                                final int CONTENT_START_Y = HEADER_HEIGHT + 17; // base for both columns
                                final int SECTION_GAP = 16; // after section title
                                final int LINE_HEIGHT = 30; // left column rhythm

                                // LEFT: base stats
                                int leftX = 20;
                                int statsY = CONTENT_START_Y;
                                int maxStat = RagnarConfigs.SERVER.caps.maxStatValue.get();

                                g.drawString(mc.font, Component.translatable("screen.ragnarmmo.stats.base_title"),
                                                leftX, statsY, 0xFFFFAA00, true);
                                statsY += SECTION_GAP;

                                g.drawString(mc.font,
                                                Component.translatable("screen.ragnarmmo.stats.points",
                                                                stats.getStatPoints(), stats.getSkillPoints()),
                                                leftX, statsY,
                                                0xFF00FF00, false);
                                statsY += LINE_HEIGHT;

                                // compute maxWidth for bars
                                int maxWidth = 0;
                                for (var key : BASE_STATS) {
                                        int value = getStatValue(stats, key);
                                        Component statLine = getStatName(key).copy().append(": " + value);
                                        maxWidth = Math.max(maxWidth, mc.font.width(statLine));
                                }

                                // clear rect caches
                                plusRect.clear();
                                minusRect.clear();
                                statRowY.clear();

                                // Draw base stats rows and store their Y
                                for (var key : BASE_STATS) {
                                        statRowY.put(key, statsY);

                                        int value = getStatValue(stats, key);
                                        int cost = value >= maxStat ? 0 : StatCost.costToIncrease(value);
                                        boolean canSpend = value < maxStat && stats.getStatPoints() >= cost;
                                        boolean canRefund = value > 1;

                                        g.fill(leftX - 2, statsY - 2, leftX + maxWidth + 60, statsY + 12, 0x44000000);

                                        Component statName = getStatName(key);
                                        g.drawString(mc.font, statName.copy().append(":"), leftX, statsY, 0xFFCCCCCC,
                                                        false);
                                        g.drawString(mc.font, String.valueOf(value), leftX + 75, statsY, 0xFFFFFFFF,
                                                        true);

                                        // Break down total stats into class/job, equipment, and temporary
                                        // buffs so the player can see where power is coming from.
                                        int equipBonus = RoAttributeApplier.getTotalBonus(player, key);
                                        var attrInstance = player.getAttribute(getForgeAttribute(key));
                                        int totalValue = attrInstance != null ? (int) Math.round(attrInstance.getValue()) : value;
                                        int jobBonus = getJobBonusValue(attrInstance, key);
                                        int trackedBuffBonus = stats.getBonus(key);
                                        int otherBonus = totalValue - value - jobBonus - equipBonus - trackedBuffBonus;
                                        int buffValue = trackedBuffBonus + otherBonus;
                                        int totalBonus = totalValue - value;

                                        if (totalBonus != 0) {
                                                int bonusX = leftX + 92; // Bonus column
                                                int bonusColor = totalBonus > 0 ? 0xFF00FF00 : 0xFFFF0000;
                                                String bonusText = (totalBonus > 0 ? "+" : "") + totalBonus;
                                                g.drawString(mc.font, bonusText, bonusX, statsY, bonusColor, true);

                                                Rect rBonusHover = new Rect(leftX, statsY - 2, 120, 12);
                                                if (rBonusHover.contains(mx, my)) {
                                                        this.deferredTooltip = java.util.List.of(
                                                            formatStatBreakdown("screen.ragnarmmo.stats.total", totalValue, ChatFormatting.GOLD),
                                                            formatStatBreakdown("screen.ragnarmmo.stats.base", value, ChatFormatting.WHITE),
                                                            formatStatBreakdown("screen.ragnarmmo.stats.job_bonus", jobBonus, ChatFormatting.LIGHT_PURPLE),
                                                            formatStatBreakdown("screen.ragnarmmo.stats.equip_bonus", equipBonus, ChatFormatting.GREEN),
                                                            formatStatBreakdown("screen.ragnarmmo.stats.buff_bonus", buffValue, ChatFormatting.AQUA)
                                                        );
                                                }
                                        }

                                        g.pose().pushPose();
                                        g.pose().translate(leftX + 118, statsY + 2, 0); // Cost text
                                        g.pose().scale(0.7f, 0.7f, 1.0f);
                                        if (value < maxStat) {
                                                g.drawString(mc.font, String.format(Locale.ROOT, "(%d pts)", cost), 0,
                                                                0, 0xFF888888, false);
                                        } else {
                                                g.drawString(mc.font,
                                                                Component.translatable("screen.ragnarmmo.stats.max"), 0,
                                                                0, 0xFFFFD700, false);
                                        }
                                        g.pose().popPose();

                                        int buttonX = leftX + 155; // Buttons
                                        Rect rMinus = new Rect(buttonX, statsY - 1, BTN_SIZE_SMALL, BTN_SIZE_SMALL);
                                        Rect rPlus = new Rect(buttonX + 16, statsY - 1, BTN_SIZE_SMALL, BTN_SIZE_SMALL);
                                        minusRect.put(key, rMinus);
                                        plusRect.put(key, rPlus);

                                        boolean hoverMinus = rMinus.contains(mx, my);
                                        boolean hoverPlus = rPlus.contains(mx, my);

                                        drawSmallButton(g, rMinus, "-", hoverMinus, canRefund);
                                        drawSmallButton(g, rPlus, "+", hoverPlus, canSpend);

                                        if (hoverPlus && !canSpend) {
                                                this.deferredTooltip = java.util.List.of(
                                                                statName.copy()
                                                                                .withStyle(ChatFormatting.YELLOW),
                                                                createTooltipLine(value, maxStat, cost));
                                        }

                                        statsY += LINE_HEIGHT;
                                }

                                // RIGHT: derived stats compressed to STR..LUK span, but with MORE SPACING via
                                // weighted intervals
                                int rightX = PANEL_WIDTH / 2 + 40;

                                // Use server-synced derived stats if available. 
                                // No local fallback to StatComputer.compute to maintain server authority.
                                var d = DerivedStatsClientCache.get();
                                boolean hasData = (d != null);

                                // Lines:
                                // 0 Offense
                                // 1 ATK
                                // 2 MATK
                                // 3 HIT
                                // 4 CRIT
                                // 5 ASPD
                                // 6 Defense
                                // 7 DEF
                                // 8 MDEF
                                // 9 HP
                                // 10 SP
                                // 11 FLEE
                                // 12 Perfect Dodge
                                // 13 Weight (header)
                                // 14 CUR
                                // 15 HIGH
                                // 16 MAX
                                final int LINES = 17;

                                Integer yStr = statRowY.get(StatKeys.STR);
                                Integer yLuk = statRowY.get(StatKeys.LUK);

                                if (yStr != null && yLuk != null && yLuk > yStr) {
                                        int span = yLuk - yStr;

                                        // ---- YOU CONTROL SPACING HERE ----
                                        final float GAP_AFTER_HEADER = 1.6f;
                                        final float GAP_BETWEEN_SECTIONS = 1.9f;
                                        final float GAP_NORMAL = 1.0f;
                                        // ---------------------------------

                                        float[] w = new float[LINES - 1];
                                        for (int j = 0; j < w.length; j++)
                                                w[j] = GAP_NORMAL;

                                        w[0] = GAP_AFTER_HEADER;
                                        w[5] = GAP_BETWEEN_SECTIONS;
                                        w[6] = GAP_AFTER_HEADER;
                                        w[12] = GAP_BETWEEN_SECTIONS;
                                        w[13] = GAP_AFTER_HEADER;

                                        float totalW = 0.0f;
                                        for (float v : w)
                                                totalW += v;

                                        float unit = span / totalW;

                                        int[] yLine = new int[LINES];
                                        float acc = 0.0f;
                                        yLine[0] = yStr;
                                        for (int li = 1; li < LINES; li++) {
                                                acc += w[li - 1] * unit;
                                                yLine[li] = Math.round(yStr + acc);
                                        }
                                        yLine[LINES - 1] = yLuk;

                                        g.pose().pushPose();
                                        g.pose().translate(rightX, 0, 0);

                                        g.drawString(mc.font,
                                                        Component.translatable(
                                                                         "screen.ragnarmmo.derived.offense_title"),
                                                         0, yLine[0], 0xFFFF8800, true);
                                        
                                        renderDerivedStat(g, 0, yLine[1], "ATK",
                                                        hasData ? String.format(Locale.ROOT, "%.1f", d.physicalAttack) : "--",
                                                        0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[2], "MATK",
                                                        hasData ? String.format(Locale.ROOT, "%.1f", d.magicAttack) : "--", 0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[3], "HIT",
                                                        hasData ? String.format(Locale.ROOT, "%.0f", d.accuracy) : "--",
                                                        0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[4], "CRIT",
                                                        hasData ? String.format(Locale.ROOT, "%.1f%%", d.criticalChance * 100) : "--",
                                                        0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[5], "ASPD",
                                                        hasData ? String.format(Locale.ROOT, "%.0f", d.attackSpeed) : "--", 0xFFFFFFFF);

                                        g.drawString(mc.font,
                                                        Component.translatable(
                                                                         "screen.ragnarmmo.derived.defense_title"),
                                                        0, yLine[6], 0xFF4488FF, true);
                                        renderDerivedStat(g, 0, yLine[7], "DEF",
                                                        hasData ? String.format(Locale.ROOT, "%.1f", d.defense) : "--", 0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[8], "MDEF",
                                                        hasData ? String.format(Locale.ROOT, "%.1f", d.magicDefense) : "--", 0xFFFFFFFF);
                                        
                                        double trueSP = (stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps)
                                                        ? ps.getMaxResource()
                                                        : stats.getManaMax();
                                        
                                        // HP and SP are base stats synced via PlayerStats, so they are always available.
                                        renderDerivedStat(g, 0, yLine[9], "HP",
                                                        String.format(Locale.ROOT, "%.0f", player.getMaxHealth()),
                                                        0xFFFF5555);
                                        renderDerivedStat(g, 0, yLine[10], "SP",
                                                        String.format(Locale.ROOT, "%.0f", trueSP), 0xFF5555FF);
                                        
                                        renderDerivedStat(g, 0, yLine[11], "FLEE",
                                                        hasData ? String.format(Locale.ROOT, "%.0f", d.flee) : "--", 0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[12], "P.DODGE",
                                                        hasData ? String.format(Locale.ROOT, "%.1f%%", d.perfectDodge * 100) : "--",
                                                        0xFFFFFFFF);

                                        // ===== Weight (Encumbrance) =====
                                        // Capacity and current weight formulas are deterministic based on base stats 
                                        // and inventory, which are already synced. We keep these local as they are 
                                        // strictly UI helpers for the inventory.
                                        int weightLimitLevel = uiGetWeightLimitLevel(player);
                                        int pushcartLevel = uiGetPushcartLevel(player);
                                        double capacity = uiComputeCapacity(stats, weightLimitLevel);
                                        double currentW = uiComputeTotalWeight(player, pushcartLevel);
                                        double highW = capacity;
                                        double maxW = capacity + WeightConstants.OVERWEIGHT_TO_MAX;

                                        int wColor;
                                        if (currentW <= highW)
                                                wColor = 0xFFFFFFFF;
                                        else if (currentW <= maxW)
                                                wColor = 0xFFFFDD55;
                                        else
                                                wColor = 0xFFFF5555;

                                        g.drawString(mc.font, Component.translatable("tooltip.ragnarmmo.weight.label"),
                                                        0, yLine[13], 0xFFAAAAAA, true);
                                        renderDerivedStat(g, 0, yLine[14], "CUR",
                                                        String.format(Locale.ROOT, "%.1f", currentW), wColor);
                                        renderDerivedStat(g, 0, yLine[15], "HIGH",
                                                        String.format(Locale.ROOT, "%.1f", highW), 0xFFFFFFFF);
                                        renderDerivedStat(g, 0, yLine[16], "MAX",
                                                        String.format(Locale.ROOT, "%.1f", maxW), 0xFFFFFFFF);

                                        g.pose().popPose();
                                } else {
                                        int derivedY = CONTENT_START_Y;
                                        g.drawString(mc.font,
                                                        Component.translatable(
                                                                         "screen.ragnarmmo.derived.offense_title"),
                                                        rightX, derivedY, 0xFFFF8800, true);
                                        derivedY += SECTION_GAP;
                                        renderDerivedStat(g, rightX, derivedY, "ATK",
                                                        hasData ? String.format(Locale.ROOT, "%.1f", d.physicalAttack) : "--",
                                                        0xFFFFFFFF);
                                }

                                // FOOTER: job info
                                int footerY = PANEL_HEIGHT - 22;

                                Component jobName = Component.translatable("job.ragnarmmo." +
                                                (stats.getJobId() == null || stats.getJobId().isBlank()
                                                                ? "novice"
                                                                : stats.getJobId().replace("ragnarmmo:", "")
                                                                                .replace("ragnarstats:", "")));

                                Component jobText = Component.translatable("screen.ragnarmmo.stats.job_format",
                                                jobName, stats.getJobLevel(), stats.getJobExp(),
                                                ExpTable.jobExpToNext(stats.getJobLevel()));

                                g.drawString(mc.font, jobText, 16, footerY, 0xFF99CCFF, false);

                                // FOOTER BUTTONS
                                // Derived from footer logic: show only if novice >= 10
                                // Removed unused footer logic for Change Class

                                boolean hoverReset = BTN_RESET_STATS.contains(mx, my);
                                drawButton(g, BTN_RESET_STATS, Component
                                                .translatable("screen.ragnarmmo.button.reset_stats"),
                                                hoverReset, true);

                                // Change Class moved to header
                        });
                }

                // HEADER BUTTONS (skills + gear + class)
                boolean hoverSkills = BTN_SKILLS.contains(mx, my);
                drawButton(g, BTN_SKILLS, Component.translatable("screen.ragnarmmo.button.skills"),
                                hoverSkills, true);

                // Show when the current job still has a valid promotion path.
                RagnarCoreAPI.get(player).ifPresent(stats -> {
                        boolean showChangeClass = JobType.fromId(stats.getJobId()).hasPromotions();
                        if (showChangeClass) {
                                boolean hoverChange = BTN_CHANGE_CLASS.contains(mx, my);
                                drawButton(g, BTN_CHANGE_CLASS,
                                                Component.translatable("screen.ragnarmmo.button.change_class"),
                                                hoverChange, true);
                        }
                });

                boolean hoverGear = BTN_GEAR.contains(mx, my);
                drawGearButton(g, BTN_GEAR, hoverGear, true);

                g.pose().popPose();

                // Tooltips OUTSIDE scaled pose
                if (deferredTooltip != null && !deferredTooltip.isEmpty()) {
                        g.renderComponentTooltip(this.font, deferredTooltip, mouseX, mouseY);
                }

                super.render(g, mouseX, mouseY, partialTicks);
        }

        // ===== Drawing helpers =====

        private void drawButton(GuiGraphics g, Rect r, Component label, boolean hovered, boolean enabled) {
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
                int ty = r.y + (r.h - 8) / 2;
                g.drawString(this.font, label, tx, ty, text, false);
        }

        private void drawSmallButton(GuiGraphics g, Rect r, String label, boolean hovered, boolean enabled) {
                int bg;
                if (!enabled)
                        bg = 0xFF232323;
                else
                        bg = hovered ? 0xFF505050 : 0xFF3A3A3A;

                int border = hovered ? 0xFFFFAA00 : GuiConstants.COLOR_PANEL_BORDER;
                int text = enabled ? (hovered ? 0xFFFFFFFF : 0xFFDDDDDD) : 0xFF777777;

                g.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
                g.renderOutline(r.x, r.y, r.w, r.h, border);

                int tw = this.font.width(label);
                int tx = r.x + (r.w - tw) / 2;
                int ty = r.y + (r.h - 8) / 2;
                g.drawString(this.font, label, tx, ty, text, false);
        }

        private void drawGearButton(GuiGraphics g, Rect r, boolean hovered, boolean enabled) {
                int bg;
                if (!enabled)
                        bg = 0xFF2A2A2A;
                else
                        bg = hovered ? 0xFF505050 : 0xFF3A3A3A;

                int border = hovered ? 0xFFFFAA00 : GuiConstants.COLOR_PANEL_BORDER;

                g.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
                g.renderOutline(r.x, r.y, r.w, r.h, border);

                g.blit(TEX_GEAR, r.x, r.y, 0, 0, r.w, r.h, r.w, r.h);
        }

        private void renderDerivedStat(GuiGraphics g, int x, int y, String label, String value, int valueColor) {
                var mc = Minecraft.getInstance();
                g.drawString(mc.font, label + ":", x, y, 0xFFAAAAAA, false);
                g.drawString(mc.font, value, x + 45, y, valueColor, false);
        }

        private Component createTooltipLine(int value, int maxStat, int cost) {
                if (value >= maxStat) {
                        return Component.translatable("screen.ragnarmmo.tooltip.max").withStyle(ChatFormatting.RED);
                }
                return Component.translatable("screen.ragnarmmo.tooltip.points", cost).withStyle(ChatFormatting.GRAY);
        }

        // ===== Input =====

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                double mx = (mouseX - panelX) / uiScale;
                double my = (mouseY - panelY) / uiScale;

                var mc = Minecraft.getInstance();
                var player = mc.player;

                // Header buttons
                if (BTN_GEAR.contains(mx, my)) {
                        mc.setScreen(new HudOverlayConfigScreen());
                        playClickSound(1.0f);
                        return true;
                }
                if (BTN_SKILLS.contains(mx, my)) {
                        mc.setScreen(new SkillsScreen(this));
                        playClickSound(1.0f);
                        return true;
                }

                if (player != null) {
                        var statsOpt = RagnarCoreAPI.get(player);
                        if (statsOpt.isPresent()) {
                                var stats = statsOpt.get();

                                if (BTN_RESET_STATS.contains(mx, my)) {
                                        Network.sendToServer(new PacketResetCharacter());
                                        // mc.setScreen(null); // Keep menu open
                                        playClickSound(1.0f);
                                        return true;
                                }

                                boolean showChangeClass = JobType.fromId(stats.getJobId()).hasPromotions();
                                if (showChangeClass && BTN_CHANGE_CLASS.contains(mx, my)) {
                                        mc.setScreen(new JobSelectionScreen(this));
                                        playClickSound(1.0f);
                                        return true;
                                }

                                for (var key : BASE_STATS) {
                                        Rect rMinus = minusRect.get(key);
                                        Rect rPlus = plusRect.get(key);
                                        if (rMinus == null || rPlus == null)
                                                continue;

                                        int value = getStatValue(stats, key);
                                        int maxStat = RagnarConfigs.SERVER.caps.maxStatValue.get();
                                        int cost = value >= maxStat ? 0 : StatCost.costToIncrease(value);

                                        boolean canSpend = value < maxStat && stats.getStatPoints() >= cost;
                                        boolean canRefund = value > 1;

                                        if (rMinus.contains(mx, my)) {
                                                if (canRefund) {
                                                        int qty = net.minecraft.client.gui.screens.Screen.hasControlDown() ? 10 : 1;
                                                        Network.sendToServer(new DeallocateStatPacket(key, qty));
                                                        playClickSound(0.9f);
                                                }
                                                return true;
                                        }
                                        if (rPlus.contains(mx, my)) {
                                                if (canSpend) {
                                                        int qty = net.minecraft.client.gui.screens.Screen.hasControlDown() ? 10 : 1;
                                                        Network.sendToServer(new AllocateStatPacket(key, qty));
                                                        playClickSound(1.0f);
                                                }
                                                return true;
                                        }
                                }
                        }
                }

                return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (com.etema.ragnarmmo.client.Keybinds.OPEN_STATS.matches(keyCode, scanCode)) {
                        Minecraft.getInstance().setScreen(null);
                        return true;
                }

                if (keyCode == 256) { // ESC
                        Minecraft.getInstance().setScreen(null);
                        return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void playClickSound(float pitch) {
                Minecraft.getInstance().getSoundManager()
                                .play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                                                .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, pitch));
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }

        // ===== Original computation helpers =====


        private static int getStatValue(IPlayerStats stats, StatKeys key) {
                return switch (key) {
                        case STR -> stats.getSTR();
                        case AGI -> stats.getAGI();
                        case VIT -> stats.getVIT();
                        case INT -> stats.getINT();
                        case DEX -> stats.getDEX();
                        case LUK -> stats.getLUK();
                        default -> throw new IllegalStateException("Unhandled StatKeys: " + key);
                };
        }

        private static Attribute getForgeAttribute(StatKeys key) {
                return switch (key) {
                        case STR -> RagnarAttributes.STR.get();
                        case AGI -> RagnarAttributes.AGI.get();
                        case VIT -> RagnarAttributes.VIT.get();
                        case INT -> RagnarAttributes.INT.get();
                        case DEX -> RagnarAttributes.DEX.get();
                        case LUK -> RagnarAttributes.LUK.get();
                        default -> throw new IllegalStateException("Unhandled StatKeys: " + key);
                };
        }

        private static int getJobBonusValue(AttributeInstance attrInstance, StatKeys key) {
                if (attrInstance == null) {
                        return 0;
                }

                AttributeModifier modifier = attrInstance.getModifier(getJobBonusUuid(key));
                return modifier == null ? 0 : (int) Math.round(modifier.getAmount());
        }

        private static java.util.UUID getJobBonusUuid(StatKeys key) {
                return switch (key) {
                        case STR -> JobBonusService.JOB_BONUS_STR;
                        case AGI -> JobBonusService.JOB_BONUS_AGI;
                        case VIT -> JobBonusService.JOB_BONUS_VIT;
                        case INT -> JobBonusService.JOB_BONUS_INT;
                        case DEX -> JobBonusService.JOB_BONUS_DEX;
                        case LUK -> JobBonusService.JOB_BONUS_LUK;
                        default -> throw new IllegalStateException("Unhandled StatKeys: " + key);
                };
        }

        private static Component formatStatBreakdown(String key, int value, ChatFormatting color) {
                String text = value > 0 ? "+" + value : String.valueOf(value);
                return Component.translatable(key, text).withStyle(color);
        }

        private static Component getStatName(StatKeys key) {
                return Component.translatable("stat.ragnarmmo." + key.id());
        }
}
