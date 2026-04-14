package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntryValidator;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobSaveEntryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ManualMobEditorScreen extends Screen {

    public enum EditorContext {
        EDIT_EXISTING_INTERNAL("Editing Manual Data"),
        CREATE_FROM_EFFECTIVE_PROFILE("Creating Override (Duplicate from Profile)"),
        CREATE_CLEAN_STUB("Creating New Override (Clean Stub)");

        private final String label;
        EditorContext(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Screen parent;
    private final ResourceLocation entityTypeId;
    private final EditorContext context;
    private long lastDetailVersion = -1;

    private CycleButton<Boolean> enabledButton;
    private EditBox levelField;
    private CycleButton<MobRank> rankButton;
    private EditBox raceField;
    private EditBox elementField;
    private EditBox sizeField;
    private EditBox maxHpField;
    private EditBox atkMinField;
    private EditBox atkMaxField;
    private EditBox defField;
    private EditBox mdefField;
    private EditBox hitField;
    private EditBox fleeField;
    private EditBox critField;
    private EditBox aspdField;
    private EditBox moveSpeedField;
    private EditBox notesField;
    
    private String feedbackMessage = "";
    private int feedbackColor = 0xFFFFFF;

    public ManualMobEditorScreen(Screen parent, ResourceLocation entityTypeId, EditorContext context) {
        super(Component.literal("Manual Mob Authoring"));
        this.parent = parent;
        this.entityTypeId = entityTypeId;
        this.context = context;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.width / 2 - 170;
        int y = 35;
        int w = 160;
        int h = 18;

        enabledButton = addRenderableWidget(CycleButton.onOffBuilder(true)
                .create(left, y, 160, 20, Component.literal("Override Status")));
        
        y += 24;
        levelField = addField(left, y, 76, h, "Level", "1");
        rankButton = addRenderableWidget(CycleButton.builder((MobRank rank) -> Component.literal(rank.name()))
                .withValues(MobRank.values())
                .withInitialValue(MobRank.NORMAL)
                .create(left + 80, y, 80, 20, Component.literal("Rank")));
        
        raceField = addField(left + 180, y, w, h, "Race", "demi_human");
        
        y += 24;
        elementField = addField(left, y, w, h, "Element", "neutral");
        sizeField = addField(left + 180, y, w, h, "Size", "medium");
        
        y += 24;
        maxHpField = addField(left, y, w, h, "Max HP", "100");
        atkMinField = addField(left + 180, y, 76, h, "Atk Min", "10");
        atkMaxField = addField(left + 180 + 84, y, 76, h, "Atk Max", "20");
        
        y += 24;
        defField = addField(left, y, 76, h, "Def", "0");
        mdefField = addField(left + 84, y, 76, h, "MDef", "0");
        hitField = addField(left + 180, y, 76, h, "Hit", "10");
        fleeField = addField(left + 180 + 84, y, 76, h, "Flee", "10");
        
        y += 24;
        critField = addField(left, y, 76, h, "Crit", "0");
        aspdField = addField(left + 84, y, 76, h, "ASPD", "130");
        moveSpeedField = addField(left + 180, y, w, h, "Move Speed", "0.23");
        
        y += 24;
        notesField = addField(left, y, 340, h, "Author Notes", "");

        addRenderableWidget(Button.builder(Component.literal("SAVE Authoritative"), b -> saveEntry())
                .bounds(left, height - 35, 120, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + 126, height - 35, 80, 20).build());

        // Pre-fill based on context
        prefillData();
    }

    private void prefillData() {
        var d = ManualMobUiState.getDetail();
        if (d == null) return;

        if (context == EditorContext.EDIT_EXISTING_INTERNAL && d.internalEntry() != null) {
            applyEntry(d.internalEntry());
        } else if (context == EditorContext.CREATE_FROM_EFFECTIVE_PROFILE && d.effectiveProfile() != null) {
            var p = d.effectiveProfile();
            levelField.setValue(String.valueOf(p.level()));
            rankButton.setValue(p.rank());
            raceField.setValue(p.race());
            elementField.setValue(p.element());
            sizeField.setValue(p.size());
            maxHpField.setValue(String.valueOf(p.maxHp()));
            atkMinField.setValue(String.valueOf(p.atkMin()));
            atkMaxField.setValue(String.valueOf(p.atkMax()));
            defField.setValue(String.valueOf(p.def()));
            mdefField.setValue(String.valueOf(p.mdef()));
            hitField.setValue(String.valueOf(p.hit()));
            fleeField.setValue(String.valueOf(p.flee()));
            critField.setValue(String.valueOf(p.crit()));
            aspdField.setValue(String.valueOf(p.aspd()));
            moveSpeedField.setValue(String.valueOf(p.moveSpeed()));
            notesField.setValue("Duplicated from active profile.");
        }
    }

    private void applyEntry(InternalManualMobEntry e) {
        enabledButton.setValue(e.enabled());
        levelField.setValue(String.valueOf(e.level()));
        rankButton.setValue(e.rank());
        raceField.setValue(e.race());
        elementField.setValue(e.element());
        sizeField.setValue(e.size());
        maxHpField.setValue(String.valueOf(e.maxHp()));
        atkMinField.setValue(String.valueOf(e.atkMin()));
        atkMaxField.setValue(String.valueOf(e.atkMax()));
        defField.setValue(String.valueOf(e.def()));
        mdefField.setValue(String.valueOf(e.mdef()));
        hitField.setValue(String.valueOf(e.hit()));
        fleeField.setValue(String.valueOf(e.flee()));
        critField.setValue(String.valueOf(e.crit()));
        aspdField.setValue(String.valueOf(e.aspd()));
        moveSpeedField.setValue(String.valueOf(e.moveSpeed()));
        notesField.setValue(e.notes());
    }

    private void saveEntry() {
        try {
            var entry = new InternalManualMobEntry(
                    entityTypeId,
                    enabledButton.getValue(),
                    Integer.parseInt(levelField.getValue()),
                    rankButton.getValue(),
                    raceField.getValue(),
                    elementField.getValue(),
                    sizeField.getValue(),
                    Integer.parseInt(maxHpField.getValue()),
                    Integer.parseInt(atkMinField.getValue()),
                    Integer.parseInt(atkMaxField.getValue()),
                    Integer.parseInt(defField.getValue()),
                    Integer.parseInt(mdefField.getValue()),
                    Integer.parseInt(hitField.getValue()),
                    Integer.parseInt(fleeField.getValue()),
                    Integer.parseInt(critField.getValue()),
                    Integer.parseInt(aspdField.getValue()),
                    Double.parseDouble(moveSpeedField.getValue()),
                    notesField.getValue(),
                    "client-sent", // Server will sanitize this
                    0L           // Server will sanitize this
            );

            // Immediate Client Validation
            InternalManualMobEntryValidator.validateOrThrow(entry);
            
            // Rule 5: canonical round-trip. We mark catalog dirty but the inspector refresh will come from server detail response.
            Network.sendToServer(new ManualMobSaveEntryPacket(entry));
            ManualMobUiState.markCatalogDirty();
            
            feedbackMessage = "Data sent to server for verification...";
            feedbackColor = 0x66FF66;
            
        } catch (Exception ex) {
            feedbackMessage = "Validation Error: " + (ex.getMessage() != null ? ex.getMessage() : "Invalid data");
            feedbackColor = 0xFF6666;
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Check for success feedback from state
        if (ManualMobUiState.getDetailVersion() != lastDetailVersion) {
            lastDetailVersion = ManualMobUiState.getDetailVersion();
            feedbackMessage = "SERVER AUTHORITY: Saved Successfully!";
            feedbackColor = 0x66FF66;
        }
    }

    private EditBox addField(int x, int y, int width, int height, String hint, String defaultValue) {
        EditBox box = new EditBox(font, x, y, width, height, Component.literal(hint));
        box.setValue(defaultValue);
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = width / 2;
        graphics.drawCenteredString(font, title, centerX, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, context.getLabel(), centerX, 20, 0xFFCC66);

        if (!feedbackMessage.isEmpty()) {
            graphics.drawCenteredString(font, feedbackMessage, centerX, height - 55, feedbackColor);
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
