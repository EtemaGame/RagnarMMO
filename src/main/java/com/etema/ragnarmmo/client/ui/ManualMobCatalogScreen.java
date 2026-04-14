package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogEntry;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.ManualMobCatalogRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManualMobCatalogScreen extends Screen {

    private static final int PAGE_SIZE = 10;

    private enum FilterMode {
        ALL("All Mobs"),
        UNDEFINED("Undefined Only"),
        MANUAL("Manual Overrides"),
        DATAPACK("Datapack Coverage");

        private final String label;
        FilterMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Screen parent;
    private final List<ManualMobCatalogEntry> allEntries = new ArrayList<>();
    private final List<ManualMobCatalogEntry> filteredEntries = new ArrayList<>();
    private EditBox searchBox;
    private FilterMode filterMode = FilterMode.ALL;
    private int page = 0;
    private long lastCatalogVersion = -1;
    private final List<Button> rowButtons = new ArrayList<>();
    private Button filterButton;
    private Button prevButton;
    private Button nextButton;

    public ManualMobCatalogScreen(Screen parent) {
        super(Component.literal("Manual Mob Bestiary"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // UI Governance: Screens can be opened, but authoritative logic resides on server responses.
        // We still check discovery here for early exit if possible.
        
        allEntries.clear();
        filteredEntries.clear();
        rowButtons.clear();

        int left = this.width / 2 - 160;
        searchBox = new EditBox(this.font, left, 24, 220, 20, Component.literal("Search"));
        searchBox.setResponder(s -> applyFilter());
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> requestCatalog())
                .bounds(left + 226, 24, 94, 20)
                .build());

        filterButton = addRenderableWidget(Button.builder(Component.literal(filterMode.getLabel()), b -> {
            filterMode = FilterMode.values()[(filterMode.ordinal() + 1) % FilterMode.values().length];
            b.setMessage(Component.literal(filterMode.getLabel()));
            applyFilter();
        }).bounds(left, 46, 120, 20).build());

        int rowY = 70;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int rowIndex = i;
            Button row = Button.builder(Component.literal("-"), b -> openRow(rowIndex))
                    .bounds(left, rowY + i * 20, 320, 18)
                    .build();
            rowButtons.add(addRenderableWidget(row));
        }

        prevButton = addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (page > 0) {
                page--;
                refreshRows();
            }
        }).bounds(left, this.height - 30, 80, 20).build());

        nextButton = addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            page++;
            refreshRows();
        }).bounds(left + 86, this.height - 30, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left + 236, this.height - 30, 80, 20).build());

        requestCatalog();
    }

    private void requestCatalog() {
        Network.sendToServer(new ManualMobCatalogRequestPacket());
    }

    private void applyCatalogData() {
        allEntries.clear();
        allEntries.addAll(ManualMobUiState.getCatalog().stream()
                .sorted(Comparator.comparing(ManualMobCatalogEntry::entityTypeId))
                .toList());
        applyFilter();
    }

    private void applyFilter() {
        filteredEntries.clear();
        String needle = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        
        for (ManualMobCatalogEntry entry : allEntries) {
            // Priority Filter Mode
            boolean matchMode = switch (filterMode) {
                case ALL -> true;
                case UNDEFINED -> !entry.internalPresent();
                case MANUAL -> entry.internalPresent();
                case DATAPACK -> entry.datapackCoverage();
            };
            if (!matchMode) continue;

            // Search Needle
            String full = (entry.entityTypeId() + " " + entry.namespace() + " " + entry.displayName()).toLowerCase(Locale.ROOT);
            if (needle.isBlank() || full.contains(needle)) {
                filteredEntries.add(entry);
            }
        }
        page = 0;
        refreshRows();
    }

    private void refreshRows() {
        int maxPage = Math.max(0, (filteredEntries.size() - 1) / PAGE_SIZE);
        if (page > maxPage) {
            page = maxPage;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < rowButtons.size(); i++) {
            int idx = start + i;
            Button row = rowButtons.get(i);
            if (idx < filteredEntries.size()) {
                ManualMobCatalogEntry entry = filteredEntries.get(idx);
                row.visible = true;
                row.active = true;
                
                // Rich Rendering with Flags (I=Internal, D=Datapack, E=Enabled/ManualEffective)
                String flags = "[" 
                        + (entry.internalPresent() ? "I" : ".") 
                        + (entry.datapackCoverage() ? "D" : ".") 
                        + (entry.manualEffective() ? "E" : ".") 
                        + "]";
                row.setMessage(Component.literal(flags + " " + entry.displayName() + " (" + entry.namespace() + ")"));
            } else {
                row.visible = false;
                row.active = false;
            }
        }
        prevButton.active = page > 0;
        nextButton.active = (page + 1) * PAGE_SIZE < filteredEntries.size();
    }

    @Override
    public void tick() {
        super.tick();
        // Rule 5: Canonical refresh from server versioning
        if (ManualMobUiState.getCatalogVersion() != lastCatalogVersion || ManualMobUiState.isCatalogDirty()) {
            lastCatalogVersion = ManualMobUiState.getCatalogVersion();
            applyCatalogData();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, "Global Registry Bestiary (Source: Registry + Server Backend)", width / 2 - 160, 100 + PAGE_SIZE*20 - 180, 0xAAAAAA, false);
        graphics.drawString(font, String.format(Locale.ROOT, "Total: %d | Page: %d/%d", filteredEntries.size(), page + 1, Math.max(1, (filteredEntries.size() + PAGE_SIZE - 1) / PAGE_SIZE)), width / 2 - 160, height - 44, 0xAAAAAA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void openRow(int rowIndex) {
        int idx = page * PAGE_SIZE + rowIndex;
        if (idx >= 0 && idx < filteredEntries.size() && minecraft != null) {
            ResourceLocation id = ResourceLocation.tryParse(filteredEntries.get(idx).entityTypeId());
            if (id != null) {
                minecraft.setScreen(new ManualMobInspectorScreen(this, id));
            }
        }
    }
}
