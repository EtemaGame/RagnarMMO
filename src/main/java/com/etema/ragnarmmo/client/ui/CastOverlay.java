package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.ClientCastManager;
import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
import com.etema.ragnarmmo.client.hud.HudRenderUtil;
import com.etema.ragnarmmo.client.hud.HudWidgetState;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class CastOverlay implements IGuiOverlay {
    public static final CastOverlay INSTANCE = new CastOverlay();

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 10;
    private static final int LABEL_GAP = 3;

    private CastOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get() || !RagnarConfigs.CLIENT.hud.cast.enabled.get())
            return;

        ClientCastManager cm = ClientCastManager.getInstance();
        if (!cm.isCasting())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null)
            return;

        RagnarConfigs.Client.Hud.HudComponent config = RagnarConfigs.CLIENT.hud.cast;
        HudWidgetState state = HudConfigSerializer.read(config);
        if (!state.enabled()) {
            return;
        }
        int width = getCastWidth();
        int height = getCastHeight(mc.font);
        HudLayoutManager.HudBounds bounds = HudLayoutManager.bounds(state, width, height, screenWidth, screenHeight);

        RenderSystem.enableBlend();

        HudLayoutManager.renderBackground(graphics, state, bounds);
        HudLayoutManager.pushWidgetTransform(graphics, bounds);

        renderCastBar(graphics, mc.font, width, cm.getProgress(), resolveSkillName(cm.getCastingSkillId()));

        HudLayoutManager.popWidgetTransform(graphics);

        RenderSystem.disableBlend();
    }

    public static int getCastWidth() {
        return BAR_WIDTH;
    }

    public static int getCastHeight(Font font) {
        return font.lineHeight + LABEL_GAP + BAR_HEIGHT;
    }

    public static int renderPreview(GuiGraphics graphics, Font font, int width) {
        return renderCastBar(graphics, font, width, 0.65f,
                Component.translatable("screen.ragnarmmo.overlay.cast.preview"));
    }

    private static int renderCastBar(GuiGraphics graphics, Font font, int width, float progress, Component skillName) {
        int labelW = font.width(skillName);
        graphics.drawString(font, skillName, (width - labelW) / 2, 0, 0xFFFFFFFF, true);

        int barY = font.lineHeight + LABEL_GAP;
        HudRenderUtil.drawGradientBar(graphics, 0, barY, width, BAR_HEIGHT, Mth.clamp(progress, 0.0f, 1.0f),
                0xFFFFFFAA, 0xFFDDBB00);

        return getCastHeight(font);
    }

    private static Component resolveSkillName(ResourceLocation skillId) {
        if (skillId == null) {
            return Component.translatable("screen.ragnarmmo.overlay.cast.unknown");
        }

        return SkillRegistry.get(skillId)
                .map(def -> def.getTranslatedName())
                .orElseGet(() -> Component.translatable(
                        "screen.ragnarmmo.overlay.cast.skill_fallback", skillId.getPath()));
    }
}
