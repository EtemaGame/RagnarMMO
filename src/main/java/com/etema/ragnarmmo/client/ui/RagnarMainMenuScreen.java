package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RagnarMainMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;

    public RagnarMainMenuScreen() {
        super(Component.translatable("gui.ragnarmmo.menu.title"));
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int y = this.height / 2 - 82;
        int buttonX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.character"),
                        button -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.skills"),
                        button -> Minecraft.getInstance().setScreen(new SkillsScreen(this)))
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.achievements"),
                        button -> Minecraft.getInstance().setScreen(new AchievementScreen()))
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.bestiary"),
                        button -> Minecraft.getInstance().setScreen(new BestiaryScreen()))
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + GAP;

        Button party = Button.builder(Component.translatable("gui.ragnarmmo.menu.party"),
                        button -> {})
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        party.active = false;
        addRenderableWidget(party);
        y += BUTTON_HEIGHT + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.settings"),
                        button -> Minecraft.getInstance().setScreen(new HudOverlayConfigScreen()))
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        y += BUTTON_HEIGHT + GAP;

        addRenderableWidget(Button.builder(Component.translatable("gui.ragnarmmo.menu.close"),
                        button -> this.onClose())
                .bounds(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = this.height / 2 - 118;
        int panelH = 235;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, 0xDD111217);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 28, 0xEE242832);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 10, 0xFFE9D8A6);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
