package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.ui.ManualMobEditorScreen;
import com.etema.ragnarmmo.client.ui.ManualMobUiState;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobDeleteEntryPacket;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobDetailRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
public class ManualMobInspectorScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation entityTypeId;
    private long lastDetailVersion = -1;
    
    // Auth-governed buttons
    private Button editButton;
    private Button createButton;
    private Button deleteButton;

    public ManualMobInspectorScreen(Screen parent, ResourceLocation entityTypeId) {
        super(Component.literal("Manual Mob Inspector"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 110;
        int bottomY = this.height - 30;

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> requestDetail())
                .bounds(left, bottomY - 24, 60, 20).build());

        editButton = addRenderableWidget(Button.builder(Component.literal("Edit Manual"), b -> openEditor(ManualMobEditorScreen.EditorContext.EDIT_EXISTING_INTERNAL))
                .bounds(left + 64, bottomY - 24, 76, 20).build());
        
        createButton = addRenderableWidget(Button.builder(Component.literal("Create Override"), b -> {
            var detail = ManualMobUiState.getDetail();
            var context = (detail != null && detail.effectiveProfile() != null) 
                    ? ManualMobEditorScreen.EditorContext.CREATE_FROM_EFFECTIVE_PROFILE 
                    : ManualMobEditorScreen.EditorContext.CREATE_CLEAN_STUB;
            openEditor(context);
        }).bounds(left + 64, bottomY - 24, 96, 20).build());

        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
            Network.sendToServer(new ManualMobDeleteEntryPacket(entityTypeId));
            ManualMobUiState.markCatalogDirty();
        }).bounds(left + 164, bottomY - 24, 56, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + 144, bottomY - 24, 66, 20).build()); // Adjusting layout for better spacing

        // Start request
        requestDetail();
        updateButtons();
    }

    private void openEditor(ManualMobEditorScreen.EditorContext context) {
        if (minecraft != null) {
            minecraft.setScreen(new ManualMobEditorScreen(this, entityTypeId, context));
        }
    }

    private void requestDetail() {
        Network.sendToServer(new ManualMobDetailRequestPacket(entityTypeId));
    }

    private void updateButtons() {
        var d = ManualMobUiState.getDetail();
        if (d == null || !d.internalPresent() && d.internalEntry() == null && d.effectiveProfile() == null && d.scalingMode() == null) {
            editButton.visible = false;
            createButton.visible = false;
            deleteButton.visible = false;
            return;
        }

        // Rule 1 & 2: Follow server action flags
        editButton.visible = d.internalPresent();
        editButton.active = d.canEdit();
        
        createButton.visible = !d.internalPresent();
        createButton.active = d.canCreate();
        
        deleteButton.visible = d.internalPresent();
        deleteButton.active = d.canDelete();
    }

    @Override
    public void tick() {
        super.tick();
        var d = ManualMobUiState.getDetail();
        if (d != null && ManualMobUiState.getDetailVersion() != lastDetailVersion) {
            lastDetailVersion = ManualMobUiState.getDetailVersion();
            updateButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = width / 2;
        graphics.drawCenteredString(font, title, centerX, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, "ID: " + entityTypeId, centerX, 20, 0xD0D0D0);

        var d = ManualMobUiState.getDetail();
        if (d == null) {
            graphics.drawCenteredString(font, "Waiting for server canonical detail...", centerX, 60, 0xAAAAAA);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int y = 35;
        // LAYER 1: EFFECTIVE RUNTIME
        graphics.drawString(font, "== EFFECTIVE RUNTIME PROFILE ==", centerX - 110, y, 0x66FF66, false);
        y += 12;
        if (d.effectiveProfile() != null) {
            var p = d.effectiveProfile();
            graphics.drawString(font, "Level: " + p.level() + " | Rank: " + p.rank(), centerX - 100, y, 0xAAAAAA, false);
            y += 10;
            graphics.drawString(font, "HP: " + p.maxHp() + " | ATK: " + p.atkMin() + "-" + p.atkMax(), centerX - 100, y, 0xAAAAAA, false);
            y += 10;
            graphics.drawString(font, "DEF: " + p.def() + " | MDEF: " + p.mdef(), centerX - 100, y, 0xAAAAAA, false);
            y += 10;
            graphics.drawString(font, "Race/Elem: " + p.race() + " / " + p.element(), centerX - 100, y, 0xAAAAAA, false);
        } else {
            graphics.drawString(font, "No active profile (Not spawned or covered)", centerX - 100, y, 0x888888, false);
        }
        y += 15;

        // LAYER 2: INTERNAL EDITABLE ENTRY
        graphics.drawString(font, "== INTERNAL MANUAL ENTRY ==", centerX - 110, y, 0xFFCC66, false);
        y += 12;
        if (d.internalPresent() && d.internalEntry() != null) {
            var e = d.internalEntry();
            graphics.drawString(font, "Enabled: " + (e.enabled() ? "YES" : "NO"), centerX - 100, y, 0xAAAAAA, false);
            y += 10;
            graphics.drawString(font, "Edited By: " + e.lastEditedBy(), centerX - 100, y, 0xAAAAAA, false);
            y += 10;
            graphics.drawString(font, "Custom Notes: " + (e.notes().isEmpty() ? "none" : e.notes()), centerX - 100, y, 0xAAAAAA, false);
        } else {
            graphics.drawString(font, "None (Using automatic or datapack values)", centerX - 100, y, 0x888888, false);
        }
        y += 15;

        // LAYER 3: SYSTEM INFO
        graphics.drawString(font, "== SYSTEM METADATA ==", centerX - 110, y, 0x66CCFF, false);
        y += 12;
        graphics.drawString(font, "Backend: " + d.effectiveBackend().toUpperCase() + " | Scaling: " + d.scalingMode().toUpperCase(), centerX - 100, y, 0xAAAAAA, false);
        y += 10;
        graphics.drawString(font, "Reason: " + (d.backendReason().isEmpty() ? "default" : d.backendReason()), centerX - 100, y, 0x888888, false);
        y += 10;
        graphics.drawString(font, "Datapack Coverage: " + (d.datapackCoverage() ? "YES" : "NO"), centerX - 100, y, 0xAAAAAA, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
