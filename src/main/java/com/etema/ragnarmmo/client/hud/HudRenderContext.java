package com.etema.ragnarmmo.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared render inputs passed through HUD widget renderers.
 */
public record HudRenderContext(
        GuiGraphics graphics,
        Font font,
        Minecraft minecraft,
        int screenWidth,
        int screenHeight,
        float partialTick) {
}
