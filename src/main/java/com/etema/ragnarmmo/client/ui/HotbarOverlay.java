package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Skill Hotbar overlay — renders all 6 skill slots in a single bar
 * directly above the vanilla hotbar (fixed position, centered).
 */
public class HotbarOverlay implements IGuiOverlay {

    public static final HotbarOverlay INSTANCE = new HotbarOverlay();

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int TOTAL_SLOTS = 6;

    /** Vertical gap between the top of vanilla hotbar and our skill bar. */
    private static final int HOTBAR_GAP = 12;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator())
            return;

        boolean combatMode = com.etema.ragnarmmo.client.ClientEvents.isCombatMode();

        // Calculate total skill bar size
        int totalWidth = TOTAL_SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;

        // Center horizontally, position just above the vanilla hotbar
        int x = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 22 - SLOT_SIZE - HOTBAR_GAP;

        // Combat Mode Indicator
        if (combatMode) {
            String combatText = "§lCOMBAT MODE";
            int tw = mc.font.width(combatText);
            guiGraphics.drawString(mc.font, combatText, (screenWidth - tw) / 2, y - 12, 0xFFFF5555, true);
        }

        // Optional background panel
        int bgAlpha = combatMode ? 160 : 100;
        int bgColor = (bgAlpha << 24) | (combatMode ? 0x220000 : 0x000000);
        guiGraphics.fill(x - 2, y - 2, x + totalWidth + 2, y + SLOT_SIZE + 2, bgColor);

        PlayerSkillsProvider.get(mc.player).ifPresent(skills -> {
            String[] hotbar = skills.getHotbar();

            for (int i = 0; i < TOTAL_SLOTS; i++) {
                int sx = x + i * (SLOT_SIZE + SLOT_SPACING);
                int sy = y;

                // Draw slot background (outlined box) - Reddish if in combat mode
                int outlineColor = combatMode ? 0x80FF5555 : 0x4DFFFFFF;
                guiGraphics.renderOutline(sx, sy, SLOT_SIZE, SLOT_SIZE, outlineColor);

                // Draw Skill Icon
                if (i < hotbar.length) {
                    String skillId = hotbar[i];
                    if (skillId != null && !skillId.isEmpty()) {
                        ResourceLocation skillLoc = ResourceLocation.tryParse(skillId);
                        if (skillLoc == null) {
                            skillLoc = new ResourceLocation("ragnarmmo", skillId);
                        }
                        ResourceLocation icon = SkillRegistry.get(skillLoc)
                                .map(ISkillDefinition::getIcon)
                                .orElse(null);
                        if (icon != null) {
                            RenderSystem.setShaderTexture(0, icon);
                            guiGraphics.blit(icon, sx + 2, sy + 2, 0, 0, 16, 16, 16, 16);
                        }

                        // Draw cooldown
                        float cdProgress = skills.getCooldownProgress(skillLoc, partialTick);
                        if (cdProgress > 0) {
                            int h = (int) (16 * cdProgress);
                            guiGraphics.fill(sx + 2, sy + 2 + (16 - h), sx + 18, sy + 18, 0x80FFFFFF);
                        }
                    }
                }

                // Draw Key number
                guiGraphics.drawString(mc.font, String.valueOf(i + 1), sx + 1, sy + 1, 0xFFE0E0E0, true);
            }
        });
    }

    public static int getWidth() {
        return TOTAL_SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
    }

    public static int getWidth(int count) {
        return count * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
    }

    public static int getHeight() {
        return SLOT_SIZE;
    }

    /**
     * Renders a simplified preview of hotbar slots for the config screen.
     * Does not require player skills data.
     */
    public static void renderPreview(GuiGraphics graphics, int startIndex, int count) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < count; i++) {
            int sx = i * (SLOT_SIZE + SLOT_SPACING);
            graphics.renderOutline(sx, 0, SLOT_SIZE, SLOT_SIZE, 0x4DFFFFFF);
            graphics.drawString(mc.font, String.valueOf(startIndex + i + 1), sx + 1, 1, 0xFFE0E0E0, true);
        }
    }
}
