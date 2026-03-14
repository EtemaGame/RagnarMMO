package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.skills.ISkillDefinition;
import com.etema.ragnarmmo.common.api.skills.SkillType;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.skills.data.SkillRegistry;
import com.etema.ragnarmmo.system.stats.net.PacketChangeJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.List;

/**
 * JobSelectionScreen - RPG-style Class Selection UI inspired by Ragnarok
 * Online.
 */
public class JobSelectionScreen extends Screen {

    // === LAYOUT CONSTANTS ===
    private static final int WINDOW_WIDTH = 480;
    private static final int WINDOW_HEIGHT = 320;
    private final Screen parent;
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 45;
    private static final int LEFT_PANEL_WIDTH = 140;

    // Textures removed
    @SuppressWarnings("removal")
    private static final ResourceLocation SLOT_EMPTY = new ResourceLocation("ragnarmmo",
            "textures/gui/skills/slot_empty.png");
    @SuppressWarnings("removal")
    private static final ResourceLocation SLOT_HOVER = new ResourceLocation("ragnarmmo",
            "textures/gui/skills/slot_hover.png");

    private JobType selectedJob = null;
    private Button confirmButton;

    // Window position
    private int windowX;
    private int windowY;

    // Popup state
    private Component popupError = null;
    private int popupTimer = 0;

    public JobSelectionScreen(Screen parent) {
        super(Component.literal("Class Selection"));
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
        this.confirmButton = Button.builder(Component.literal("Change Job"), b -> {
            if (selectedJob != null) {
                boolean hasUnspentPoints = false;
                boolean lowJobLevel = false;

                if (this.minecraft != null && this.minecraft.player != null) {
                    var statsOpt = com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(this.minecraft.player);
                    if (statsOpt.isPresent()) {
                        hasUnspentPoints = statsOpt.get().getSkillPoints() > 0;
                        if ("ragnarmmo:novice".equals(statsOpt.get().getJobId()) && statsOpt.get().getJobLevel() < 10) {
                            lowJobLevel = true;
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
        g.drawString(this.font, "Class Selection", windowX + 15, windowY + 12, 0xFFFFAA00, true);
        g.drawString(this.font, "— Choose your path", windowX + 110, windowY + 12, 0xFF888888, false);

        // === LEFT PANEL: Job Slots ===
        int panelX = windowX + 15;
        int panelY = windowY + HEADER_HEIGHT + 10;
        int panelWidth = LEFT_PANEL_WIDTH - 10;
        int panelHeight = WINDOW_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 20;

        // Panel background
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x66000000);
        renderBorder(g, panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);

        // Job slots
        int slotY = panelY + 8;
        int slotHeight = 28;
        int slotGap = 5;

        List<JobType> eligibleJobs = getEligibleJobs();

        for (JobType job : eligibleJobs) {
            boolean isHovered = mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                    mouseY >= slotY && mouseY <= slotY + slotHeight;
            boolean isSelected = selectedJob == job;

            // Slot background
            int bgColor = isSelected ? 0xDD1A1A2E : (isHovered ? 0xAA2A2A4E : 0x88000000);
            g.fill(panelX + 5, slotY, panelX + panelWidth - 5, slotY + slotHeight, bgColor);

            // Slot border
            int borderColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFAAAAAA : 0xFF444444);
            renderBorder(g, panelX + 5, slotY, panelX + panelWidth - 5, slotY + slotHeight, borderColor);

            // Job name
            int textColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            g.drawString(this.font, job.getDisplayName(), panelX + 12, slotY + 10, textColor, isSelected);

            slotY += slotHeight + slotGap;
        }

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
            int contentY = rightY + 15;

            // === Job Title ===
            g.fill(contentX - 5, contentY - 3, rightX + rightWidth - 15, contentY + 14, 0x44FFAA00);
            g.drawString(this.font, selectedJob.getDisplayName(), contentX, contentY, 0xFFFFD700, true);
            contentY += 22;

            // === Role Description ===
            g.drawString(this.font, "Role:", contentX, contentY, 0xFF888888, false);
            g.drawString(this.font, getRoleDescription(selectedJob), contentX + 35, contentY, 0xFFFFFFFF, false);
            contentY += 14;

            // === Primary Stats ===
            g.drawString(this.font, "Primary:", contentX, contentY, 0xFF888888, false);
            String primaryStats = getPrimaryStats(selectedJob);
            g.drawString(this.font, primaryStats, contentX + 50, contentY, 0xFF00FF00, false);
            contentY += 12;

            // === Stat Growth (Max Job Bonus) ===
            g.drawString(this.font, "Bonus:", contentX, contentY, 0xFF888888, false);
            com.etema.ragnarmmo.common.api.stats.Stats6 maxBonus = com.etema.ragnarmmo.system.stats.progression.JobBonusData
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

            String finalBonus = bonusStr.isEmpty() ? "None" : bonusStr.toString().trim();
            g.drawString(this.font, finalBonus, contentX + 50, contentY, 0xFF55FF55, false);
            contentY += 18;

            // === Flavor Description ===
            String desc = getClassDescription(selectedJob);
            List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(Component.literal(desc),
                    rightWidth - 40);
            for (net.minecraft.util.FormattedCharSequence line : descLines) {
                g.drawString(this.font, line, contentX, contentY, 0xFFAAAAAA, false);
                contentY += 11;
            }
            contentY += 6;

            // === Skills Section ===
            g.fill(contentX - 5, contentY - 3, contentX + 140, contentY + 12, 0x33FFAA00);
            g.drawString(this.font, "Class Skills:", contentX, contentY, 0xFFFFAA00, true);
            contentY += 18;

            var skills = selectedJob.getAllowedSkills();
            int skillCount = 0;
            for (SkillType skill : skills) {
                if (skillCount >= 6) {
                    g.drawString(this.font, "... and more", contentX + 10, contentY, 0xFF666666, false);
                    break;
                }

                // Look up display name from SkillRegistry
                String skillDisplayName = SkillRegistry.get(skill.toResourceLocation())
                        .map(ISkillDefinition::getDisplayName)
                        .orElse(skill.getId());

                double mult = selectedJob.getXpMultiplier(skill);
                boolean isHovered2 = mouseX >= contentX && mouseX <= rightX + rightWidth - 20 &&
                        mouseY >= contentY && mouseY <= contentY + 12;

                int skillColor = isHovered2 ? 0xFFFFFFFF : 0xFFCCCCCC;
                g.drawString(this.font, "• " + skillDisplayName, contentX + 10, contentY, skillColor, false);

                if (isHovered2) {
                    renderSkillTooltip(g, skill, mouseX, mouseY);
                }

                contentY += 14;
                skillCount++;
            }

            if (skills.isEmpty()) {
                g.drawString(this.font, "Uses advanced skill tree", contentX + 10, contentY, 0xFF666666, false);
            }

            // === Warning ===
            int warningY = rightY + rightHeight - 50;
            if (warningY > contentY + 10) {
                g.fill(rightX + 5, warningY - 3, rightX + rightWidth - 5, warningY + 35, 0x40FF0000);
                renderBorder(g, rightX + 5, warningY - 3, rightX + rightWidth - 5, warningY + 35, 0xFFFF4444);
                g.drawString(this.font, "\u26A0 WARNING", rightX + 12, warningY, 0xFFFF4444, true);
                g.drawString(this.font, "Level & Job Level reset to 1.", rightX + 12, warningY + 12, 0xFFFFAAAA, false);
                g.drawString(this.font, "Stats remain allocated.", rightX + 12, warningY + 23, 0xFFFFAAAA, false);
            }

        } else {
            // No job selected
            g.drawCenteredString(this.font, "Select a class to view details",
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

            int slotY = panelY + 8;
            int slotHeight = 28;
            int slotGap = 5;

            List<JobType> eligibleJobs = getEligibleJobs();

            for (JobType job : eligibleJobs) {
                if (mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                        mouseY >= slotY && mouseY <= slotY + slotHeight) {
                    this.selectedJob = job;
                    updateConfirmButton();
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                slotY += slotHeight + slotGap;
            }
        }
        return false;
    }

    private void renderSkillTooltip(GuiGraphics g, SkillType skill, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        var defOpt = SkillRegistry.get(skill.toResourceLocation());
        String displayName = defOpt.map(ISkillDefinition::getDisplayName).orElse(skill.getId());
        String scalingStat = defOpt.map(ISkillDefinition::getScalingStat).orElse("STR");
        lines.add(Component.literal(displayName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.translatable("skill.ragnarmmo." + skill.getId() + ".desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Primary: " + scalingStat).withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private void renderBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color); // Top
        g.fill(x1, y2 - 1, x2, y2, color); // Bottom
        g.fill(x1, y1, x1 + 1, y2, color); // Left
        g.fill(x2 - 1, y1, x2, y2, color); // Right
    }

    private String getRoleDescription(JobType job) {
        return switch (job) {
            case SWORDSMAN -> "Melee Tank / DPS";
            case MAGE -> "Magic Burst DPS";
            case ARCHER -> "Ranged Physical DPS";
            case MERCHANT -> "Utility & Trading";
            case THIEF -> "Evasion & Speed";
            case ACOLYTE -> "Support & Healing";
            case KNIGHT -> "Heavy Tank / AoE DPS";
            case WIZARD -> "Arcane Burst / AoE DPS";
            case HUNTER -> "Ranged DPS / Trapper";
            case PRIEST -> "Healer / Buffer";
            default -> "Beginner class";
        };
    }

    private String getPrimaryStats(JobType job) {
        return switch (job) {
            case SWORDSMAN -> "STR, VIT";
            case MAGE -> "INT, DEX";
            case ARCHER -> "DEX, AGI";
            case MERCHANT -> "LUK, STR";
            case THIEF -> "AGI, LUK";
            case ACOLYTE -> "INT, VIT";
            case KNIGHT -> "STR, VIT, DEX";
            case WIZARD -> "INT, DEX";
            case HUNTER -> "DEX, AGI, INT";
            case PRIEST -> "INT, VIT, DEX";
            default -> "All";
        };
    }

    private String getClassDescription(JobType job) {
        return switch (job) {
            case SWORDSMAN ->
                "Front-line warriors who excel at close combat. They possess high vitality and can wield swords with lethal precision.";
            case MAGE ->
                "Scholars of the arcane arts who channel elemental magic. Fragile but devastating from range with powerful spells.";
            case ARCHER ->
                "Masters of ranged combat who strike from afar with unmatched precision. Agile and deadly with a bow.";
            case THIEF ->
                "Swift and cunning fighters who rely on speed, evasion, and critical strikes. Experts in poison and stealth.";
            case MERCHANT ->
                "Resourceful traders with surprising combat ability. They specialize in item crafting, overcharging, and support.";
            case ACOLYTE ->
                "Devoted servants of the divine, channeling holy power to heal allies and ward off darkness.";
            case KNIGHT ->
                "Elite mounted warriors clad in heavy armor. Masters of spear and sword, capable of devastating charge attacks.";
            case WIZARD ->
                "Supreme arcane casters who command ice, lightning, and fire. Their spells can annihilate entire groups of enemies.";
            case HUNTER ->
                "Expert marksmen who set traps and command falcons. They combine ranged attacks with tactical area control.";
            case PRIEST ->
                "Holy healers who protect allies with blessings, powerful heals, and sacred sanctuaries that purify the battlefield.";
            default -> "A beginning adventurer.";
        };
    }
}
