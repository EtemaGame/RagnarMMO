package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.ClientCastManager;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class CastBarOverlay implements IGuiOverlay {
    public static final CastBarOverlay INSTANCE = new CastBarOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        ClientCastManager manager = ClientCastManager.getInstance();
        if (!manager.isCasting())
            return;

        ResourceLocation skillId = manager.getCastingSkillId();
        if (skillId == null)
            return;

        float progress = manager.getProgress();
        // progress goes 0.0 -> 1.0 (start -> finish)

        int barWidth = 100;
        int barHeight = 5;
        int x = (screenWidth - barWidth) / 2;
        int y = (screenHeight / 2) + 30; // Below crosshair

        // Draw Background
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF404040);

        // Draw Progress
        int progressWidth = (int) (barWidth * progress);
        int color = 0xFF00FF00; // Green

        graphics.fill(x, y, x + progressWidth, y + barHeight, color);

        // Draw Name
        String displayName = SkillRegistry.get(skillId)
                .map(ISkillDefinition::getDisplayName)
                .orElse(skillId.getPath());
        Component name = Component.literal(displayName);
        int textWidth = Minecraft.getInstance().font.width(name);
        graphics.drawString(Minecraft.getInstance().font, name, (screenWidth - textWidth) / 2, y - 10, 0xFFFFFF);

        // Draw Timer (Optional)
        String time = String.format("%.1f", (manager.getCurrentTicks() / 20.0f));
        graphics.drawString(Minecraft.getInstance().font, time, x + barWidth + 5, y - 2, 0xFFFFFF);
    }
}
