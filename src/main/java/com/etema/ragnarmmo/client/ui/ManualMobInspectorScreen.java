package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobDetailRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ManualMobInspectorScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation entityTypeId;
    private Button openEditorButton;

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
        addRenderableWidget(Button.builder(Component.literal("Refresh detail"), b -> requestDetail())
                .bounds(left, y, 220, 20).build());
        openEditorButton = addRenderableWidget(Button.builder(Component.literal("Open editor"), b -> {
                    if (MobStatsConfigAccess.isManualMobEditorEnabled()) {
                        minecraft.setScreen(new ManualMobEditorScreen(this, entityTypeId));
                    }
                })
                .bounds(left, y + 24, 220, 20).build());
        openEditorButton.active = MobStatsConfigAccess.isManualMobEditorEnabled();
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left, this.height - 28, 80, 20).build());
        requestDetail();
    }

    private void requestDetail() {
        if (MobStatsConfigAccess.isManualMobDiscoveryEnabled()) {
            Network.sendToServer(new ManualMobDetailRequestPacket(entityTypeId));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.drawString(this.font, "Entity: " + entityTypeId, this.width / 2 - 110, 22, 0xD0D0D0, false);
        var entry = ManualMobUiState.getDetail();
        if (entry != null && entry.entityTypeId().equals(entityTypeId)) {
            graphics.drawString(this.font, "enabled=" + entry.enabled() + " level=" + entry.level() + " rank=" + entry.rank().name().toLowerCase(), this.width / 2 - 110, 80, 0xAAAAAA, false);
            graphics.drawString(this.font, "atk=" + entry.atkMin() + "-" + entry.atkMax() + " def=" + entry.def() + " mdef=" + entry.mdef(), this.width / 2 - 110, 94, 0xAAAAAA, false);
        }
        if (!MobStatsConfigAccess.isManualMobEditorEnabled()) {
            graphics.drawString(this.font, "Editor disabled by config.", this.width / 2 - 110, 118, 0xFFAA66, false);
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
