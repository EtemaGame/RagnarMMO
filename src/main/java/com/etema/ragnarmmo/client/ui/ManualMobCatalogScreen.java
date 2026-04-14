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

    private final Screen parent;
    private final List<ManualMobCatalogEntry> allEntries = new ArrayList<>();
    private final List<ManualMobCatalogEntry> filteredEntries = new ArrayList<>();
    private EditBox searchBox;
    private int page = 0;
    private final List<Button> rowButtons = new ArrayList<>();
    private Button prevButton;
    private Button nextButton;

    public ManualMobCatalogScreen(Screen parent) {
        super(Component.literal("Manual Mob Catalog"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (!MobStatsConfigAccess.isManualMobDiscoveryEnabled()) {
            onClose();
            return;
        }
        allEntries.clear();
        filteredEntries.clear();
        rowButtons.clear();

        int left = this.width / 2 - 160;
        searchBox = new EditBox(this.font, left, 24, 220, 20, Component.literal("Search"));
        searchBox.setResponder(this::applyFilter);
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> requestCatalog())
                .bounds(left + 226, 24, 90, 20)
                .build());

        int rowY = 56;
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
        applyCatalogData();
    }

    private void applyCatalogData() {
        allEntries.clear();
        allEntries.addAll(ManualMobUiState.getCatalog().stream()
                .sorted(Comparator.comparing(ManualMobCatalogEntry::entityTypeId))
                .toList());
        applyFilter(searchBox == null ? "" : searchBox.getValue());
    }

    private void applyFilter(String text) {
        filteredEntries.clear();
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            filteredEntries.addAll(allEntries);
        } else {
            for (ManualMobCatalogEntry entry : allEntries) {
                String full = (entry.entityTypeId() + " " + entry.namespace() + " " + entry.displayName()).toLowerCase(Locale.ROOT);
                if (full.contains(needle)) {
                    filteredEntries.add(entry);
                }
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
                row.setMessage(Component.literal(entry.entityTypeId() + " [" + entry.effectiveBackend() + "]"));
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
        if (allEntries.size() != ManualMobUiState.getCatalog().size()) {
            applyCatalogData();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, "Data-driven catalog (INTERNAL/DATAPACK coverage + backend).", width / 2 - 160, 46, 0xAAAAAA, false);
        graphics.drawString(font, String.format(Locale.ROOT, "Total: %d | Page: %d", filteredEntries.size(), page + 1), width / 2 - 160, height - 44, 0xAAAAAA, false);
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
