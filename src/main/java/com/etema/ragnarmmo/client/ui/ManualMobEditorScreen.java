package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ManualMobEditorScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation entityTypeId;
    private final List<EditBox> fields = new ArrayList<>();
    private EditBox level;
    private EditBox rank;
    private EditBox race;
    private EditBox element;
    private EditBox size;
    private EditBox maxHp;
    private EditBox atkMin;
    private EditBox atkMax;
    private EditBox def;
    private EditBox mdef;
    private EditBox hit;
    private EditBox flee;
    private EditBox crit;
    private EditBox aspd;
    private EditBox moveSpeed;
    private EditBox notes;

    public ManualMobEditorScreen(Screen parent, ResourceLocation entityTypeId) {
        super(Component.literal("Manual Mob Editor"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 170;
        int y = 26;
        int w = 160;
        int h = 18;

        level = addField(left, y, w, h, "level", "1");
        rank = addField(left + 180, y, w, h, "rank", "normal");
        race = addField(left, y + 22, w, h, "race", "demi_human");
        element = addField(left + 180, y + 22, w, h, "element", "neutral");
        size = addField(left, y + 44, w, h, "size", "medium");
        maxHp = addField(left + 180, y + 44, w, h, "max_hp", "50");

        atkMin = addField(left, y + 66, w, h, "atk_min", "5");
        atkMax = addField(left + 180, y + 66, w, h, "atk_max", "8");
        def = addField(left, y + 88, w, h, "def", "0");
        mdef = addField(left + 180, y + 88, w, h, "mdef", "0");
        hit = addField(left, y + 110, w, h, "hit", "10");
        flee = addField(left + 180, y + 110, w, h, "flee", "5");
        crit = addField(left, y + 132, w, h, "crit", "1");
        aspd = addField(left + 180, y + 132, w, h, "aspd", "140");
        moveSpeed = addField(left, y + 154, w, h, "move_speed", "0.23");
        notes = addField(left + 180, y + 154, w, h, "notes", "");

        addRenderableWidget(Button.builder(Component.literal("Save (send commands)"), b -> saveAll())
                .bounds(left, this.height - 30, 170, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + 180, this.height - 30, 80, 20).build());
    }

    private EditBox addField(int x, int y, int width, int height, String hint, String defaultValue) {
        EditBox box = new EditBox(this.font, x, y, width, height, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setValue(defaultValue);
        fields.add(addRenderableWidget(box));
        return box;
    }

    private void saveAll() {
        ManualMobClientCommandBridge.sendCommand("mobmanual create " + entityTypeId);
        sendEdit("level", level.getValue());
        sendEdit("rank", rank.getValue());
        sendEdit("race", race.getValue());
        sendEdit("element", element.getValue());
        sendEdit("size", size.getValue());
        sendEdit("max_hp", maxHp.getValue());
        sendEdit("atk_min", atkMin.getValue());
        sendEdit("atk_max", atkMax.getValue());
        sendEdit("def", def.getValue());
        sendEdit("mdef", mdef.getValue());
        sendEdit("hit", hit.getValue());
        sendEdit("flee", flee.getValue());
        sendEdit("crit", crit.getValue());
        sendEdit("aspd", aspd.getValue());
        sendEdit("move_speed", moveSpeed.getValue());
        sendEdit("notes", notes.getValue());
        ManualMobClientCommandBridge.sendCommand("mobmanual enable " + entityTypeId);
    }

    private void sendEdit(String field, String value) {
        String sanitized = value == null ? "" : value.replace("\"", "");
        ManualMobClientCommandBridge.sendCommand("mobmanual edit " + entityTypeId + " " + field + " " + sanitized);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.drawString(this.font, "Entity: " + entityTypeId, this.width / 2 - 170, 14, 0xD0D0D0, false);
        graphics.drawString(this.font, "Tip: this GUI sends /mobmanual commands to the server.", this.width / 2 - 170, this.height - 44, 0xAAAAAA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : fields) {
            if (field.isFocused()) {
                return field.keyPressed(keyCode, scanCode, modifiers) || field.canConsumeInput();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox field : fields) {
            if (field.isFocused() && field.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
