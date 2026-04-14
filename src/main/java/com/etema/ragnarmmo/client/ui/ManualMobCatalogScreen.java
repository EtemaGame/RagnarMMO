package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManualMobCatalogScreen extends Screen {

    private static final int PAGE_SIZE = 10;

    private final Screen parent;
    private final List<ResourceLocation> allLivingIds = new ArrayList<>();
    private final List<ResourceLocation> filteredIds = new ArrayList<>();
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
        allLivingIds.clear();
        filteredIds.clear();
        rowButtons.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER) {
                continue;
            }
            MobCategory category = type.getCategory();
            if (category != MobCategory.MISC) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id != null) {
                    allLivingIds.add(id);
                }
            }
        }
        allLivingIds.sort(Comparator.comparing(ResourceLocation::toString));
        filteredIds.addAll(allLivingIds);

        int left = this.width / 2 - 160;
        searchBox = new EditBox(this.font, left, 24, 220, 20, Component.literal("Search"));
        searchBox.setResponder(value -> applyFilter(value));
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> applyFilter(searchBox.getValue()))
                .bounds(left + 226, 24, 90, 20)
                .build());

        int rowY = 56;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int rowIndex = i;
            Button row = Button.builder(Component.literal("-"), b -> openRow(rowIndex))
                    .bounds(left, rowY + i * 20, 250, 18)
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

        refreshRows();
    }

    private void applyFilter(String text) {
        filteredIds.clear();
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            filteredIds.addAll(allLivingIds);
        } else {
            for (ResourceLocation id : allLivingIds) {
                String full = id.toString().toLowerCase(Locale.ROOT);
                if (full.contains(needle)) {
                    filteredIds.add(id);
                }
            }
        }
        page = 0;
        refreshRows();
    }

    private void refreshRows() {
        int maxPage = Math.max(0, (filteredIds.size() - 1) / PAGE_SIZE);
        if (page > maxPage) {
            page = maxPage;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < rowButtons.size(); i++) {
            int idx = start + i;
            Button row = rowButtons.get(i);
            if (idx < filteredIds.size()) {
                ResourceLocation id = filteredIds.get(idx);
                row.visible = true;
                row.active = true;
                row.setMessage(Component.literal(id.toString()));
            } else {
                row.visible = false;
                row.active = false;
            }
        }
        prevButton.active = page > 0;
        nextButton.active = (page + 1) * PAGE_SIZE < filteredIds.size();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, "Use search to filter entity_type ids. Click one to inspect/edit.", width / 2 - 160, 46, 0xAAAAAA, false);
        graphics.drawString(font, String.format(Locale.ROOT, "Total: %d | Page: %d", filteredIds.size(), page + 1), width / 2 - 160, height - 44, 0xAAAAAA, false);
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
        if (idx >= 0 && idx < filteredIds.size() && minecraft != null) {
            minecraft.setScreen(new ManualMobInspectorScreen(this, filteredIds.get(idx)));
        }
    }
}
