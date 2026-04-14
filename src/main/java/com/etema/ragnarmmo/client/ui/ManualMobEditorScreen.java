package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntryValidator;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobDeleteEntryPacket;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobDetailRequestPacket;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobSaveEntryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private String validationError = "";

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

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAll())
                .bounds(left, this.height - 30, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteEntry())
                .bounds(left + 116, this.height - 30, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + 202, this.height - 30, 80, 20).build());

        Network.sendToServer(new ManualMobDetailRequestPacket(entityTypeId));
        loadFromDetail();
    }

    @Override
    public void tick() {
        super.tick();
        loadFromDetail();
    }

    private void loadFromDetail() {
        InternalManualMobEntry detail = ManualMobUiState.getDetail();
        if (detail == null || !detail.entityTypeId().equals(entityTypeId)) {
            return;
        }
        if (!level.getValue().equals(Integer.toString(detail.level()))) {
            level.setValue(Integer.toString(detail.level()));
            rank.setValue(detail.rank().name().toLowerCase(Locale.ROOT));
            race.setValue(detail.race());
            element.setValue(detail.element());
            size.setValue(detail.size());
            maxHp.setValue(Integer.toString(detail.maxHp()));
            atkMin.setValue(Integer.toString(detail.atkMin()));
            atkMax.setValue(Integer.toString(detail.atkMax()));
            def.setValue(Integer.toString(detail.def()));
            mdef.setValue(Integer.toString(detail.mdef()));
            hit.setValue(Integer.toString(detail.hit()));
            flee.setValue(Integer.toString(detail.flee()));
            crit.setValue(Integer.toString(detail.crit()));
            aspd.setValue(Integer.toString(detail.aspd()));
            moveSpeed.setValue(Double.toString(detail.moveSpeed()));
            notes.setValue(detail.notes());
        }
    }

    private EditBox addField(int x, int y, int width, int height, String hint, String defaultValue) {
        EditBox box = new EditBox(this.font, x, y, width, height, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setValue(defaultValue);
        fields.add(addRenderableWidget(box));
        return box;
    }

    private void saveAll() {
        if (!MobStatsConfigAccess.isManualMobEditorEnabled()) {
            validationError = "Manual editor disabled by config.";
            return;
        }
        try {
            InternalManualMobEntry entry = new InternalManualMobEntry(
                    entityTypeId,
                    true,
                    Integer.parseInt(level.getValue()),
                    MobRank.valueOf(rank.getValue().trim().toUpperCase(Locale.ROOT)),
                    race.getValue(),
                    element.getValue(),
                    size.getValue(),
                    Integer.parseInt(maxHp.getValue()),
                    Integer.parseInt(atkMin.getValue()),
                    Integer.parseInt(atkMax.getValue()),
                    Integer.parseInt(def.getValue()),
                    Integer.parseInt(mdef.getValue()),
                    Integer.parseInt(hit.getValue()),
                    Integer.parseInt(flee.getValue()),
                    Integer.parseInt(crit.getValue()),
                    Integer.parseInt(aspd.getValue()),
                    Double.parseDouble(moveSpeed.getValue()),
                    notes.getValue(),
                    "ui",
                    System.currentTimeMillis());
            InternalManualMobEntryValidator.validateOrThrow(entry);
            validationError = "";
            Network.sendToServer(new ManualMobSaveEntryPacket(entry));
        } catch (Exception ex) {
            validationError = ex.getMessage() == null ? "Invalid input" : ex.getMessage();
        }
    }

    private void deleteEntry() {
        if (!MobStatsConfigAccess.isManualMobEditorEnabled()) {
            validationError = "Manual editor disabled by config.";
            return;
        }
        Network.sendToServer(new ManualMobDeleteEntryPacket(entityTypeId));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.drawString(this.font, "Entity: " + entityTypeId, this.width / 2 - 170, 14, 0xD0D0D0, false);
        if (!validationError.isBlank()) {
            graphics.drawString(this.font, validationError, this.width / 2 - 170, this.height - 44, 0xFF6666, false);
        }
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
