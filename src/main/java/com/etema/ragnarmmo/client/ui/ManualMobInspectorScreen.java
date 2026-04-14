package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ManualMobInspectorScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation entityTypeId;
    private final List<String> lastActions = new ArrayList<>();

    public ManualMobInspectorScreen(Screen parent, ResourceLocation entityTypeId) {
        super(Component.literal("Manual Mob Inspector"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 110;
        int y = 40;
        addRenderableWidget(Button.builder(Component.literal("Run inspect command"), b -> run("mobmanual inspect " + entityTypeId))
                .bounds(left, y, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Create/ensure internal stub"), b -> run("mobmanual create " + entityTypeId))
                .bounds(left, y + 24, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Enable entry"), b -> run("mobmanual enable " + entityTypeId))
                .bounds(left, y + 48, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Disable entry"), b -> run("mobmanual disable " + entityTypeId))
                .bounds(left + 114, y + 48, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Open editor"), b -> minecraft.setScreen(new ManualMobEditorScreen(this, entityTypeId)))
                .bounds(left, y + 72, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left, this.height - 28, 80, 20).build());
    }

    private void run(String command) {
        boolean sent = ManualMobClientCommandBridge.sendCommand(command);
        lastActions.add((sent ? "✔ " : "✖ ") + "/" + command);
        while (lastActions.size() > 6) {
            lastActions.remove(0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.drawString(this.font, "Entity: " + entityTypeId, this.width / 2 - 110, 22, 0xD0D0D0, false);
        graphics.drawString(this.font, "Actions log:", this.width / 2 - 110, 140, 0xAAAAAA, false);
        int y = 154;
        for (String line : lastActions) {
            graphics.drawString(this.font, line, this.width / 2 - 110, y, 0xAAAAAA, false);
            y += 12;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
