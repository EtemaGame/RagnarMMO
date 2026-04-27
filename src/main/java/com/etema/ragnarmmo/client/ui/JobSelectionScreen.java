package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.player.stats.network.PacketChangeJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JobSelectionScreen - RPG-style Class Selection UI inspired by Ragnarok
 * Online.
 */
public class JobSelectionScreen extends Screen {
    private static final ResourceLocation BASIC_SKILL = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "basic_skill");

    // === LAYOUT CONSTANTS ===
    private static final int WINDOW_WIDTH = 480;
    private static final int WINDOW_HEIGHT = 320;
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 45;
    private static final int LEFT_PANEL_WIDTH = 140;
    private static final int JOB_SLOT_HEIGHT = 28;
    private static final int JOB_SLOT_GAP = 5;
    private static final int DETAIL_TOP_PADDING = 15;
    private static final int DETAIL_BOTTOM_PADDING = 8;
    private static final int DETAIL_LINE_HEIGHT = 11;
    private static final int SCROLL_STEP = 15;
    private final Screen parent;

    private JobType selectedJob = null;
    private Button confirmButton;

    private double leftScrollAmount = 0;
    private double rightScrollAmount = 0;

    // Window position
    private int windowX;
    private int windowY;

    // Popup state
    private Component popupError = null;
    private int popupTimer = 0;

    public JobSelectionScreen(Screen parent) {
        super(Component.translatable("screen.ragnarmmo.job_selection.title"));
        this.parent = parent;
    }

    @Override
    public void tick() {
        super.tick();
        if (popupTimer > 0) {
            popupTimer--;
        }
    }

    @Override
    protected void init() {
        this.windowX = (this.width - WINDOW_WIDTH) / 2;
        this.windowY = (this.height - WINDOW_HEIGHT) / 2;

        int footerY = windowY + WINDOW_HEIGHT - FOOTER_HEIGHT + 12;

        // Back Button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        })
                .bounds(windowX + 15, footerY, 70, 18)
                .build());

        // Confirm Button
        this.confirmButton = Button.builder(Component.translatable("screen.ragnarmmo.job_selection.change_job"), b -> {
            if (selectedJob != null) {
                boolean hasUnspentPoints = false;
                boolean lowJobLevel = false;
                boolean lowBasicSkill = false;

                if (this.minecraft != null && this.minecraft.player != null) {
                    var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(this.minecraft.player);
                    if (statsOpt.isPresent()) {
                        JobType currentJob = JobType.fromId(statsOpt.get().getJobId());
                        hasUnspentPoints = statsOpt.get().getSkillPoints() > 0;
                        if (currentJob == JobType.NOVICE && statsOpt.get().getJobLevel() < 10) {
                            lowJobLevel = true;
                        }
                        if (currentJob == JobType.NOVICE) {
                            int basicSkillLevel = PlayerSkillsProvider.get(this.minecraft.player)
                                    .map(skills -> skills.getSkillLevel(BASIC_SKILL))
                                    .orElse(0);
                            lowBasicSkill = basicSkillLevel < 9;
                        }
                    }
                }

                if (lowJobLevel) {
                    this.popupError = Component.translatable("message.ragnarmmo.low_job_level");
                    this.popupTimer = 60;
                    return;
                }

                if (hasUnspentPoints) {
                    this.popupError = Component.translatable("message.ragnarmmo.unspent_skill_points");
                    this.popupTimer = 60;
                    return;
                }

                if (lowBasicSkill) {
                    this.popupError = Component.translatable("message.ragnarmmo.low_basic_skill");
                    this.popupTimer = 60;
                    return;
                }

                Network.sendToServer(new PacketChangeJob(selectedJob.getId()));
                this.onClose();
            }
        })
                .bounds(windowX + WINDOW_WIDTH - 100, footerY, 85, 18)
                .build();
        this.confirmButton.active = false;
        this.addRenderableWidget(this.confirmButton);
    }

    /**
     * Returns only the jobs the player is eligible to change to.
     * Novice -> First Classes (Swordsman, Mage, etc.)
     * First Class -> promotions (e.g., Mage -> Wizard)
     * Second Class -> empty (already at max tier)
     */
    private List<JobType> getEligibleJobs() {
        if (this.minecraft == null || this.minecraft.player == null)
            return List.of();

        var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(this.minecraft.player);
        if (statsOpt.isEmpty())
            return List.of();

        JobType currentJob = JobType.fromId(statsOpt.get().getJobId());
        return currentJob.getPromotions();
    }

    private void updateConfirmButton() {
        if (this.confirmButton != null) {
            this.confirmButton.active = selectedJob != null;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // === Layer 0: Dark Overlay ===
        g.fill(0, 0, this.width, this.height, 0x99000000);

        // === Layer 1: Window Background ===
        g.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, GuiConstants.COLOR_PANEL_BG);
        g.renderOutline(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, GuiConstants.COLOR_PANEL_BORDER);

        // === HEADER ===
        g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.title"),
                windowX + 15, windowY + 12, 0xFFFFAA00, true);
        g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.subtitle"),
                windowX + 110, windowY + 12, 0xFF888888, false);

        // === LEFT PANEL: Job Slots ===
        int panelX = windowX + 15;
        int panelY = windowY + HEADER_HEIGHT + 10;
        int panelWidth = LEFT_PANEL_WIDTH - 10;
        int panelHeight = WINDOW_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20;

        // Panel background
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x66000000);
        renderBorder(g, panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);

        // Job slots
        int slotHeight = JOB_SLOT_HEIGHT;
        int slotGap = JOB_SLOT_GAP;

        List<JobType> eligibleJobs = getEligibleJobs();
        int leftMaxScroll = getLeftMaxScroll(eligibleJobs.size(), panelHeight);
        leftScrollAmount = Mth.clamp(leftScrollAmount, 0, leftMaxScroll);
        int slotY = panelY + 8 - (int) leftScrollAmount;

        g.enableScissor(panelX, panelY, panelX + panelWidth, panelY + panelHeight);
        for (JobType job : eligibleJobs) {
            boolean isHovered = mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                    mouseY >= slotY && mouseY <= slotY + slotHeight &&
                    mouseY >= panelY && mouseY <= panelY + panelHeight;
            boolean isSelected = selectedJob == job;

            // Slot background
            int bgColor = isSelected ? 0xDD1A1A2E : (isHovered ? 0xAA2A2A4E : 0x88000000);
            g.fill(panelX + 5, slotY, panelX + panelWidth - 5, slotY + slotHeight, bgColor);

            // Slot border
            int borderColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFAAAAAA : 0xFF444444);
            renderBorder(g, panelX + 5, slotY, panelX + panelWidth - 5, slotY + slotHeight, borderColor);

            // Job name
            int textColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            g.drawString(this.font, jobName(job), panelX + 12, slotY + 10, textColor, isSelected);

            slotY += slotHeight + slotGap;
        }
        g.disableScissor();
        renderScrollBar(g, panelX + panelWidth - 4, panelY + 2, panelHeight - 4, getLeftContentHeight(eligibleJobs.size()),
                panelHeight, (int) leftScrollAmount);

        // === RIGHT PANEL: Job Details ===
        int rightX = windowX + LEFT_PANEL_WIDTH + 15;
        int rightY = windowY + HEADER_HEIGHT + 10;
        int rightWidth = WINDOW_WIDTH - LEFT_PANEL_WIDTH - 35;
        int rightHeight = WINDOW_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20;

        // Panel background
        g.fill(rightX, rightY, rightX + rightWidth, rightY + rightHeight, 0x66000000);
        renderBorder(g, rightX, rightY, rightX + rightWidth, rightY + rightHeight, 0xFF555555);

        if (selectedJob != null) {
            int contentX = rightX + 15;
            int rightMaxScroll = getRightMaxScroll(selectedJob, rightWidth, rightHeight);
            rightScrollAmount = Mth.clamp(rightScrollAmount, 0, rightMaxScroll);
            int contentY = rightY + DETAIL_TOP_PADDING - (int) rightScrollAmount;
            ResourceLocation hoveredSkill = null;

            g.enableScissor(rightX, rightY, rightX + rightWidth, rightY + rightHeight);

            // === Job Title ===
            g.fill(contentX - 5, contentY - 3, rightX + rightWidth - 15, contentY + 14, 0x44FFAA00);
            g.drawString(this.font, jobName(selectedJob), contentX, contentY, 0xFFFFD700, true);
            contentY += 22;

            // === Role Description ===
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.role"),
                    contentX, contentY, 0xFF888888, false);
            g.drawString(this.font, getRoleDescription(selectedJob), contentX + 35, contentY, 0xFFFFFFFF, false);
            contentY += 14;

            // === Primary Stats ===
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.primary"),
                    contentX, contentY, 0xFF888888, false);
            Component primaryStats = getPrimaryStats(selectedJob);
            g.drawString(this.font, primaryStats, contentX + 50, contentY, 0xFF00FF00, false);
            contentY += 12;

            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.resource"),
                    contentX, contentY, 0xFF888888, false);
            g.drawString(this.font, getResourceType(selectedJob), contentX + 60, contentY, 0xFF88CCFF, false);
            contentY += 12;

            // === Stat Growth (Max Job Bonus) ===
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.bonus"),
                    contentX, contentY, 0xFF888888, false);
            com.etema.ragnarmmo.common.api.stats.Stats6 maxBonus = com.etema.ragnarmmo.player.stats.progression.JobBonusData
                    .getBonus(selectedJob, 50);

            StringBuilder bonusStr = new StringBuilder();
            if (maxBonus.str() > 0)
                bonusStr.append("STR+").append(maxBonus.str()).append(" ");
            if (maxBonus.agi() > 0)
                bonusStr.append("AGI+").append(maxBonus.agi()).append(" ");
            if (maxBonus.vit() > 0)
                bonusStr.append("VIT+").append(maxBonus.vit()).append(" ");
            if (maxBonus.int_() > 0)
                bonusStr.append("INT+").append(maxBonus.int_()).append(" ");
            if (maxBonus.dex() > 0)
                bonusStr.append("DEX+").append(maxBonus.dex()).append(" ");
            if (maxBonus.luk() > 0)
                bonusStr.append("LUK+").append(maxBonus.luk()).append(" ");

            Component finalBonus = bonusStr.isEmpty()
                    ? Component.translatable("screen.ragnarmmo.job_selection.none")
                    : Component.translatable("screen.ragnarmmo.job_selection.bonus.list", bonusStr.toString().trim());
            g.drawString(this.font, finalBonus, contentX + 50, contentY, 0xFF55FF55, false);
            contentY += 18;

            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.reset_preview"),
                    contentX, contentY, 0xFFFFCC66, false);
            contentY += 14;

            // === Flavor Description ===
            Component desc = getClassDescription(selectedJob);
            List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(desc, rightWidth - 40);
            for (net.minecraft.util.FormattedCharSequence line : descLines) {
                g.drawString(this.font, line, contentX, contentY, 0xFFAAAAAA, false);
                contentY += DETAIL_LINE_HEIGHT;
            }
            contentY += 6;

            // === Skills Section ===
            g.fill(contentX - 5, contentY - 3, contentX + 140, contentY + 12, 0x33FFAA00);
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.class_skills"),
                    contentX, contentY, 0xFFFFAA00, true);
            contentY += 18;

            List<ResourceLocation> skills = selectedJob.getAllowedSkillIds().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
            for (ResourceLocation skill : skills) {
                // Look up display name from SkillRegistry
                Component skillDisplayName = SkillRegistry.get(skill)
                        .map(ISkillDefinition::getTranslatedName)
                        .orElseGet(() -> Component.translatable(
                                "screen.ragnarmmo.job_selection.skill_fallback", skill.getPath()));

                Component skillLine = Component.translatable("screen.ragnarmmo.job_selection.skill_bullet",
                        skillDisplayName);
                List<net.minecraft.util.FormattedCharSequence> skillLines = this.font.split(skillLine, rightWidth - 50);
                int skillBlockHeight = Math.max(12, skillLines.size() * DETAIL_LINE_HEIGHT);
                boolean isHovered2 = mouseX >= contentX && mouseX <= rightX + rightWidth - 20 &&
                        mouseY >= contentY && mouseY <= contentY + skillBlockHeight &&
                        mouseY >= rightY && mouseY <= rightY + rightHeight;

                int skillColor = isHovered2 ? 0xFFFFFFFF : 0xFFCCCCCC;
                int skillLineY = contentY;
                for (net.minecraft.util.FormattedCharSequence line : skillLines) {
                    g.drawString(this.font, line, contentX + 10, skillLineY, skillColor, false);
                    skillLineY += DETAIL_LINE_HEIGHT;
                }

                if (isHovered2) {
                    hoveredSkill = skill;
                }

                contentY += skillBlockHeight + 3;
            }

            if (skills.isEmpty()) {
                g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.advanced_skill_tree"),
                        contentX + 10, contentY, 0xFF666666, false);
                contentY += 14;
            }

            // === Warning ===
            contentY += 10;
            g.fill(rightX + 5, contentY - 3, rightX + rightWidth - 5, contentY + 35, 0x40FF0000);
            renderBorder(g, rightX + 5, contentY - 3, rightX + rightWidth - 5, contentY + 35, 0xFFFF4444);
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.warning.title"),
                    rightX + 12, contentY, 0xFFFF4444, true);
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.warning.base"),
                    rightX + 12, contentY + 12, 0xFFFFAAAA, false);
            g.drawString(this.font, Component.translatable("screen.ragnarmmo.job_selection.warning.job"),
                    rightX + 12, contentY + 23, 0xFFFFAAAA, false);

            g.disableScissor();
            renderScrollBar(g, rightX + rightWidth - 4, rightY + 2, rightHeight - 4,
                    getRightContentHeight(selectedJob, rightWidth), rightHeight, (int) rightScrollAmount);

            if (hoveredSkill != null) {
                renderSkillTooltip(g, hoveredSkill, mouseX, mouseY);
            }

        } else {
            // No job selected
            g.drawCenteredString(this.font, Component.translatable("screen.ragnarmmo.job_selection.select_class"),
                    rightX + rightWidth / 2, rightY + rightHeight / 2 - 5, 0xFF666666);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // === POPUP RENDER ===
        if (popupTimer > 0 && popupError != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);

            int popupWidth = this.font.width(popupError) + 40;
            int popupHeight = 30;
            int pX = (this.width - popupWidth) / 2;
            int pY = (this.height - popupHeight) / 2;

            float alpha = popupTimer > 10 ? 1.0f : popupTimer / 10.0f;
            int alphaInt = (int) (alpha * 255) << 24;
            int bgAlpha = (int) (alpha * 230) << 24; // Fondo semi-transparente oscuro

            g.fill(pX, pY, pX + popupWidth, pY + popupHeight, bgAlpha);
            renderBorder(g, pX, pY, pX + popupWidth, pY + popupHeight, 0xFF5555 | alphaInt);

            int colorWithAlpha = 0xFFFFFF | alphaInt;
            g.drawCenteredString(this.font, popupError, this.width / 2, pY + 11, colorWithAlpha);

            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            int panelX = windowX + 15;
            int panelY = windowY + HEADER_HEIGHT + 10;
            int panelWidth = LEFT_PANEL_WIDTH - 10;
            int panelHeight = WINDOW_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20;

            int slotHeight = JOB_SLOT_HEIGHT;
            int slotGap = JOB_SLOT_GAP;

            List<JobType> eligibleJobs = getEligibleJobs();
            int leftMaxScroll = getLeftMaxScroll(eligibleJobs.size(), panelHeight);
            leftScrollAmount = Mth.clamp(leftScrollAmount, 0, leftMaxScroll);
            int slotY = panelY + 8 - (int) leftScrollAmount;

            for (JobType job : eligibleJobs) {
                if (mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                        mouseY >= slotY && mouseY <= slotY + slotHeight &&
                        mouseY >= panelY && mouseY <= panelY + panelHeight) {
                    this.selectedJob = job;
                    this.rightScrollAmount = 0; // Reset details scroll on new job
                    updateConfirmButton();
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                slotY += slotHeight + slotGap;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelX = windowX + 15;
        int panelWidth = LEFT_PANEL_WIDTH - 10;
        int rightX = windowX + LEFT_PANEL_WIDTH + 15;
        int rightWidth = WINDOW_WIDTH - LEFT_PANEL_WIDTH - 35;
        int panelY = windowY + HEADER_HEIGHT + 10;
        int panelHeight = WINDOW_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int maxScroll = getLeftMaxScroll(getEligibleJobs().size(), panelHeight);
            this.leftScrollAmount = Mth.clamp(this.leftScrollAmount - delta * SCROLL_STEP, 0, maxScroll);
            return true;
        }

        if (mouseX >= rightX && mouseX <= rightX + rightWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int maxScroll = selectedJob == null ? 0 : getRightMaxScroll(selectedJob, rightWidth, panelHeight);
            this.rightScrollAmount = Mth.clamp(this.rightScrollAmount - delta * SCROLL_STEP, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getLeftContentHeight(int jobCount) {
        if (jobCount <= 0) {
            return 16;
        }
        return 16 + (jobCount * JOB_SLOT_HEIGHT) + ((jobCount - 1) * JOB_SLOT_GAP);
    }

    private int getLeftMaxScroll(int jobCount, int panelHeight) {
        return Math.max(0, getLeftContentHeight(jobCount) - panelHeight);
    }

    private int getRightMaxScroll(JobType job, int rightWidth, int rightHeight) {
        return Math.max(0, getRightContentHeight(job, rightWidth) - rightHeight);
    }

    private int getRightContentHeight(JobType job, int rightWidth) {
        if (job == null) {
            return 0;
        }

        int contentHeight = DETAIL_TOP_PADDING;
        contentHeight += 22; // Title
        contentHeight += 14; // Role
        contentHeight += 12; // Primary stats
        contentHeight += 12; // Resource
        contentHeight += 18; // Stat bonus
        contentHeight += 14; // Reset preview
        contentHeight += this.font.split(getClassDescription(job), rightWidth - 40).size() * DETAIL_LINE_HEIGHT + 6;
        contentHeight += 18; // Skills header

        List<ResourceLocation> skills = job.getAllowedSkillIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        for (ResourceLocation skill : skills) {
            Component skillDisplayName = SkillRegistry.get(skill)
                    .map(ISkillDefinition::getTranslatedName)
                    .orElseGet(() -> Component.translatable(
                            "screen.ragnarmmo.job_selection.skill_fallback", skill.getPath()));
            Component skillLine = Component.translatable("screen.ragnarmmo.job_selection.skill_bullet",
                    skillDisplayName);
            contentHeight += Math.max(12, this.font.split(skillLine, rightWidth - 50).size() * DETAIL_LINE_HEIGHT) + 3;
        }

        if (skills.isEmpty()) {
            contentHeight += 14;
        }

        contentHeight += 10; // Warning gap
        contentHeight += 38; // Warning box
        contentHeight += DETAIL_BOTTOM_PADDING;
        return contentHeight;
    }

    private void renderScrollBar(GuiGraphics g, int x, int y, int height, int contentHeight, int viewportHeight,
            int scrollAmount) {
        if (contentHeight <= viewportHeight || height <= 0) {
            return;
        }

        int thumbHeight = Mth.clamp((int) Math.round((double) viewportHeight / (double) contentHeight * height), 12,
                height);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int travel = Math.max(0, height - thumbHeight);
        int thumbY = y + (int) Math.round(travel * (scrollAmount / (double) maxScroll));

        g.fill(x, y, x + 2, y + height, 0x44000000);
        g.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0x99FFFFFF);
    }

    private void renderSkillTooltip(GuiGraphics g, ResourceLocation skill, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        var defOpt = SkillRegistry.get(skill);
        Component displayName = defOpt.map(ISkillDefinition::getTranslatedName)
                .orElseGet(() -> Component.translatable("screen.ragnarmmo.job_selection.skill_fallback", skill.getPath()));
        String scalingStat = defOpt.map(ISkillDefinition::getScalingStat).orElse("STR");
        lines.add(displayName.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.translatable("skill." + skill.getNamespace() + "." + skill.getPath() + ".desc")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.ragnarmmo.skills.tooltip.primary", scalingStat)
                .withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private void renderBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color); // Top
        g.fill(x1, y2 - 1, x2, y2, color); // Bottom
        g.fill(x1, y1, x1 + 1, y2, color); // Left
        g.fill(x2 - 1, y1, x2, y2, color); // Right
    }

    private static Component jobName(JobType job) {
        return Component.translatable(jobKey(job));
    }

    private static Component getRoleDescription(JobType job) {
        return Component.translatable(jobKey(job) + ".role");
    }

    private static Component getPrimaryStats(JobType job) {
        return Component.translatable(jobKey(job) + ".primary");
    }

    private static Component getResourceType(JobType job) {
        return Component.translatable("screen.ragnarmmo.job_selection.resource.sp");
    }

    private static Component getClassDescription(JobType job) {
        return Component.translatable(jobKey(job) + ".desc");
    }

    private static String jobKey(JobType job) {
        return "job.ragnarmmo." + job.getId();
    }
}
