package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Skill hotbar overlay that renders the 6 combat skill slots above the vanilla
 * hotbar.
 */
public class HotbarOverlay implements IGuiOverlay {

    public static final HotbarOverlay INSTANCE = new HotbarOverlay();

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int TOTAL_SLOTS = 6;

    /** Vertical gap between the top of vanilla hotbar and our skill bar. */
    private static final int HOTBAR_GAP = 17;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }

        boolean combatMode = com.etema.ragnarmmo.client.ClientEvents.isCombatMode();

        int totalWidth = TOTAL_SLOTS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
        int x = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 22 - SLOT_SIZE - HOTBAR_GAP;

        if (combatMode) {
            Component combatText = Component.literal("COMBAT MODE").withStyle(ChatFormatting.BOLD);
            int tw = mc.font.width(combatText);
            guiGraphics.drawString(mc.font, combatText, (screenWidth - tw) / 2, y - 12, 0xFFFF5555, true);
        }

        int bgAlpha = combatMode ? 160 : 100;
        int bgColor = (bgAlpha << 24) | (combatMode ? 0x220000 : 0x000000);
        guiGraphics.fill(x - 2, y - 2, x + totalWidth + 2, y + SLOT_SIZE + 2, bgColor);

        PlayerSkillsProvider.get(mc.player).ifPresent(skills -> {
            String[] hotbar = skills.getHotbar();

            for (int i = 0; i < TOTAL_SLOTS; i++) {
                int sx = x + i * (SLOT_SIZE + SLOT_SPACING);
                int sy = y;

                int outlineColor = combatMode ? 0x80FF5555 : 0x4DFFFFFF;
                guiGraphics.renderOutline(sx, sy, SLOT_SIZE, SLOT_SIZE, outlineColor);

                if (i < hotbar.length) {
                    String skillId = hotbar[i];
                    if (skillId != null && !skillId.isEmpty()) {
                        ResourceLocation skillLoc = ResourceLocation.tryParse(skillId);
                        if (skillLoc == null) {
                            skillLoc = ResourceLocation.fromNamespaceAndPath("ragnarmmo", skillId);
                        }
                        ResourceLocation icon = SkillRegistry.get(skillLoc)
                                .map(ISkillDefinition::getIcon)
                                .orElse(null);
                        if (icon != null) {
                            RenderSystem.setShaderTexture(0, icon);
                            guiGraphics.blit(icon, sx + 2, sy + 2, 0, 0, 16, 16, 16, 16);
                        }

                        float cdProgress = skills.getCooldownProgress(skillLoc, partialTick);
                        if (cdProgress > 0) {
                            int h = (int) (16 * cdProgress);
                            guiGraphics.fill(sx + 2, sy + 2, sx + 18, sy + 18 - h, 0x90000000);

                            int remainingTicks = (int) skills.getCooldownTicksRemaining(skillLoc);
                            if (remainingTicks > 20) {
                                String timeText = String.valueOf(remainingTicks / 20);
                                int tw = mc.font.width(timeText);
                                guiGraphics.drawString(mc.font, timeText, sx + 10 - tw / 2, sy + 6, 0xFFFFFFFF, true);
                            }
                        }
                    }
                }

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
