package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.ClientCastManager;
import com.etema.ragnarmmo.client.hud.HudRenderUtil;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class CastOverlay implements IGuiOverlay {
    public static final CastOverlay INSTANCE = new CastOverlay();

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 10;

    private CastOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        ClientCastManager cm = ClientCastManager.getInstance();
        if (!cm.isCasting())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null)
            return;

        float progress = cm.getProgress();
        ResourceLocation skillId = cm.getCastingSkillId();

        // Position: Center-bottom area (65% down the screen)
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = (int) (screenHeight * 0.65) - 5;

        RenderSystem.enableBlend();

        // Background / Panel style for the bar if needed, but drawGradientBar handled it mostly.
        // RO Colors: Yellowish/Golden for casting progress
        int colorTop = 0xFFFFFFAA;    // Light Yellow
        int colorBottom = 0xFFDDBB00; // Golden/Amber
        
        HudRenderUtil.drawGradientBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, progress, colorTop, colorBottom);

        // Draw skill name above the bar
        if (skillId != null) {
            String name = SkillRegistry.get(skillId)
                    .map(def -> def.getDisplayName())
                    .orElse(skillId.getPath());
            
            // Format name for RO feel
            String displayText = "§l" + name;
            int nameW = mc.font.width(displayText);
            graphics.drawString(mc.font, displayText, (screenWidth - nameW) / 2, y - 11, 0xFFFFFFFF, true);
        }

        RenderSystem.disableBlend();
    }
}
