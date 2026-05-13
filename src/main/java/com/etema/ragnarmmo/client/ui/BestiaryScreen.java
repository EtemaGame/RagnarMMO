package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.bestiary.api.*;
import com.etema.ragnarmmo.bestiary.data.BestiaryClientRegistry;
import com.etema.ragnarmmo.bestiary.network.RequestBestiaryDetailsPacket;
import com.etema.ragnarmmo.bestiary.network.RequestBestiaryIndexPacket;
import com.etema.ragnarmmo.common.net.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BestiaryScreen extends Screen {
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 300;
    private static final int LEFT_W = 210;
    private static final int ROW_H = 18;

    private EditBox searchBox;
    private BestiaryFilterTab selectedTab = BestiaryFilterTab.ALL;
    private ResourceLocation selectedEntity;
    private int scroll;
    private int detailScroll;
    private final Set<ResourceLocation> requestedDetails = new HashSet<>();

    public BestiaryScreen() {
        super(Component.translatable("gui.ragnarmmo.bestiary.title"));
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        this.searchBox = new EditBox(this.font, x + 10, y + 32, LEFT_W - 20, 18,
                Component.translatable("gui.ragnarmmo.bestiary.search"));
        this.searchBox.setMaxLength(80);
        addRenderableWidget(searchBox);
        requestIndexIfNeeded();
    }

    private void requestIndexIfNeeded() {
        if (!BestiaryClientRegistry.isLoaded()) {
            Network.sendToServer(new RequestBestiaryIndexPacket());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (searchBox != null) {
            searchBox.tick();
        }
        ensureSelection();
        requestSelectedDetails();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = panelX();
        int y = panelY();
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xE6101116);
        g.fill(x, y, x + PANEL_W, y + 26, 0xEE242832);
        g.drawString(this.font, this.title, x + 10, y + 9, 0xFFE9D8A6, false);
        g.drawString(this.font, Component.literal("v" + BestiaryClientRegistry.version()), x + PANEL_W - 36, y + 9, 0xFF8F98A8, false);

        drawTabs(g, mouseX, mouseY);
        drawList(g, mouseX, mouseY);
        drawDetails(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX() + 10;
        int y = panelY() + 56;
        int tabW = 62;
        int tabH = 17;
        int i = 0;
        for (BestiaryFilterTab tab : BestiaryFilterTab.values()) {
            int tx = x + (i % 3) * (tabW + 4);
            int ty = y + (i / 3) * (tabH + 3);
            boolean selected = selectedTab == tab;
            boolean hovered = contains(tx, ty, tabW, tabH, mouseX, mouseY);
            g.fill(tx, ty, tx + tabW, ty + tabH, selected ? 0xFF6C4D2E : hovered ? 0xFF2E3440 : 0xFF1A1E27);
            g.drawCenteredString(this.font,
                    Component.translatable("gui.ragnarmmo.bestiary.tab." + tab.name().toLowerCase(Locale.ROOT)),
                    tx + tabW / 2, ty + 5, 0xFFE6E6E6);
            i++;
        }
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX() + 10;
        int y = panelY() + 98;
        int h = PANEL_H - 108;
        g.fill(x, y, x + LEFT_W - 20, y + h, 0xFF171A21);

        List<BestiaryEntryDto> entries = filteredEntries();
        if (!BestiaryClientRegistry.isLoaded()) {
            g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.loading"), x + 8, y + 8, 0xFFB6BECF, false);
            return;
        }
        if (entries.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.empty"), x + 8, y + 8, 0xFFB6BECF, false);
            return;
        }

        int visibleRows = h / ROW_H;
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < visibleRows && i + scroll < entries.size(); i++) {
            BestiaryEntryDto entry = entries.get(i + scroll);
            int ry = y + i * ROW_H;
            boolean selected = entry.entityId().equals(selectedEntity);
            boolean hovered = contains(x, ry, LEFT_W - 20, ROW_H, mouseX, mouseY);
            if (selected || hovered) {
                g.fill(x + 1, ry + 1, x + LEFT_W - 21, ry + ROW_H - 1, selected ? 0xFF4B3A24 : 0xFF252A35);
            }
            drawClippedString(g, displayName(entry), x + 5, ry + 5, LEFT_W - 32, 0xFFE3E7EF);
        }
    }

    private void drawDetails(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX() + LEFT_W;
        int y = panelY() + 36;
        int w = PANEL_W - LEFT_W - 12;
        int h = PANEL_H - 46;
        g.fill(x, y, x + w, y + h, 0xFF151820);

        BestiaryEntryDto entry = selectedEntry();
        if (entry == null) {
            g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.select_entry"), x + 10, y + 10, 0xFFB6BECF, false);
            return;
        }

        int contentX = x + 10;
        int contentW = w - 20;
        int line = y + 10 - detailScroll;
        g.enableScissor(x, y, x + w, y + h);

        g.drawString(this.font, displayName(entry), contentX, line, 0xFFE9D8A6, false);
        line += 16;
        drawClippedString(g, Component.literal(entry.entityId().toString()), contentX, line, contentW, 0xFF9AA4B5);
        line += 14;
        drawVisualPreview(g, entry, x + w - 72, y + 66);
        g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.mod", entry.modId()), contentX, line, 0xFFCDD3DF, false);
        line += 12;
        g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.category",
                Component.translatable("gui.ragnarmmo.bestiary.category." + entry.category().name().toLowerCase(Locale.ROOT))),
                contentX, line, 0xFFCDD3DF, false);
        line += 70;

        BestiaryEntryDetailsDto details = BestiaryClientRegistry.details(entry.entityId()).orElse(null);
        if (details == null) {
            g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.details_loading"), contentX, line, 0xFFB6BECF, false);
            g.disableScissor();
            return;
        }

        if (!details.descriptionId().isBlank()) {
            line = drawWrapped(g, Component.translatable(details.descriptionId()), contentX, line, contentW, 0xFFCDD3DF) + 6;
        }

        line = drawStats(g, contentX, line, contentW, details.stats());
        line = drawSpawn(g, contentX, line, contentW, details.spawn());
        drawDrops(g, contentX, line, contentW, details.drops());
        g.disableScissor();
    }

    private int drawStats(GuiGraphics g, int x, int y, int width, BestiaryStatPreviewDto stats) {
        g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.section.stats"), x, y, 0xFFE9D8A6, false);
        y += 12;
        if (stats == null || !stats.hasAuthoredStats()) {
            y = drawWrapped(g, Component.translatable("gui.ragnarmmo.bestiary.no_authored_stats"), x, y, width, 0xFF9AA4B5);
            return y + 18;
        }
        y = drawWrapped(g, Component.literal("Lv " + stats.level() + " | " + stats.rank() + " | " + stats.tier()), x, y, width, 0xFFCDD3DF);
        y = drawWrapped(g, Component.literal(stats.race() + " / " + stats.element() + " / " + stats.size()), x, y, width, 0xFFCDD3DF);
        y = drawWrapped(g, Component.literal("HP " + stats.maxHp() + " | ATK " + stats.atkMin() + "-" + stats.atkMax()
                + " | DEF " + stats.def() + " | MDEF " + stats.mdef()), x, y, width, 0xFFCDD3DF);
        if (stats.runtimeScaling()) {
            y = drawWrapped(g, Component.translatable("gui.ragnarmmo.bestiary.runtime_scaling"), x, y, width, 0xFF9AA4B5);
        }
        return y + 8;
    }

    private int drawSpawn(GuiGraphics g, int x, int y, int width, BestiarySpawnInfoDto spawn) {
        g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.section.spawn"), x, y, 0xFFE9D8A6, false);
        y += 12;
        if (spawn == null || (spawn.dimensions().isEmpty() && spawn.notes().isBlank())) {
            y = drawWrapped(g, Component.translatable("gui.ragnarmmo.bestiary.not_documented"), x, y, width, 0xFF9AA4B5);
            return y + 18;
        }
        if (!spawn.dimensions().isEmpty()) {
            y = drawWrapped(g, Component.literal(joinIds(spawn.dimensions())), x, y, width, 0xFFCDD3DF);
        }
        if (!spawn.notes().isBlank()) {
            y = drawWrapped(g, Component.literal(spawn.notes()), x, y, width, 0xFFCDD3DF);
        }
        return y + 8;
    }

    private void drawDrops(GuiGraphics g, int x, int y, int width, List<BestiaryDropInfoDto> drops) {
        g.drawString(this.font, Component.translatable("gui.ragnarmmo.bestiary.section.drops"), x, y, 0xFFE9D8A6, false);
        y += 12;
        if (drops == null || drops.isEmpty()) {
            drawWrapped(g, Component.translatable("gui.ragnarmmo.bestiary.not_documented"), x, y, width, 0xFF9AA4B5);
            return;
        }
        int count = 0;
        for (BestiaryDropInfoDto drop : drops) {
            if (count++ >= 16) {
                drawWrapped(g, Component.literal("..."), x, y, width, 0xFF9AA4B5);
                break;
            }
            Component name = drop.label().isBlank() ? itemName(drop.itemId()) : Component.literal(drop.label());
            String chance = drop.chance() > 0.0D ? String.format(Locale.ROOT, " %.2f%%", drop.chance() * 100.0D) : "";
            y = drawWrapped(g, Component.literal("- ").append(name).append(" [" + drop.source().name() + "]" + chance),
                    x, y, width, 0xFFCDD3DF);
            if (!drop.noteId().isBlank()) {
                y = drawWrapped(g, Component.translatable(drop.noteId()), x + 8, y, width - 8, 0xFF9AA4B5);
            }
        }
    }

    private void drawVisualPreview(GuiGraphics g, BestiaryEntryDto entry, int centerX, int baseY) {
        g.fill(centerX - 42, baseY - 54, centerX + 42, baseY + 10, 0xFF10131A);
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entry.entityId());
        Minecraft mc = Minecraft.getInstance();
        if (type == null || mc.level == null) {
            return;
        }
        Entity entity = type.create(mc.level);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        float maxSize = Math.max(living.getBbWidth(), living.getBbHeight());
        int scale = (int) Math.max(8, Math.min(36, 28.0F / Math.max(0.65F, maxSize)));
        try {
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, centerX, baseY, scale, 25.0F, -12.0F, living);
        } catch (RuntimeException ignored) {
            // Some modded entities are not safe to render in an inventory preview.
        }
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : this.font.split(text, width)) {
            g.drawString(this.font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private void drawClippedString(GuiGraphics g, Component text, int x, int y, int width, int color) {
        String value = text.getString();
        if (this.font.width(value) <= width) {
            g.drawString(this.font, text, x, y, color, false);
            return;
        }
        String ellipsis = "...";
        while (!value.isEmpty() && this.font.width(value + ellipsis) > width) {
            value = value.substring(0, value.length() - 1);
        }
        g.drawString(this.font, Component.literal(value + ellipsis), x, y, color, false);
    }

    private List<BestiaryEntryDto> filteredEntries() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<BestiaryEntryDto> result = new ArrayList<>();
        for (BestiaryEntryDto entry : BestiaryClientRegistry.entries()) {
            if (!selectedTab.accepts(entry.category())) {
                continue;
            }
            if (!query.isBlank()
                    && !entry.entityId().toString().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.modId().toLowerCase(Locale.ROOT).contains(query)
                    && !displayName(entry).getString().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(entry);
        }
        return result;
    }

    private void ensureSelection() {
        List<BestiaryEntryDto> entries = filteredEntries();
        if (selectedEntity == null || entries.stream().noneMatch(entry -> entry.entityId().equals(selectedEntity))) {
            selectedEntity = entries.isEmpty() ? null : entries.get(0).entityId();
        }
    }

    private void requestSelectedDetails() {
        if (selectedEntity != null
                && BestiaryClientRegistry.details(selectedEntity).isEmpty()
                && requestedDetails.add(selectedEntity)) {
            Network.sendToServer(new RequestBestiaryDetailsPacket(selectedEntity));
        }
    }

    private BestiaryEntryDto selectedEntry() {
        if (selectedEntity == null) {
            return null;
        }
        return BestiaryClientRegistry.entries().stream()
                .filter(entry -> entry.entityId().equals(selectedEntity))
                .findFirst()
                .orElse(null);
    }

    private Component displayName(BestiaryEntryDto entry) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entry.entityId());
        return type != null ? Component.translatable(type.getDescriptionId()) : Component.literal(entry.entityId().toString());
    }

    private Component itemName(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return item != null ? item.getDescription() : Component.literal(itemId.toString());
    }

    private String joinIds(List<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString).reduce((a, b) -> a + ", " + b).orElse("");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int x = panelX() + 10;
        int y = panelY() + 56;
        int tabW = 62;
        int tabH = 17;
        int i = 0;
        for (BestiaryFilterTab tab : BestiaryFilterTab.values()) {
            int tx = x + (i % 3) * (tabW + 4);
            int ty = y + (i / 3) * (tabH + 3);
            if (contains(tx, ty, tabW, tabH, mouseX, mouseY)) {
                selectedTab = tab;
                scroll = 0;
                detailScroll = 0;
                ensureSelection();
                return true;
            }
            i++;
        }

        int listX = panelX() + 10;
        int listY = panelY() + 98;
        int listH = PANEL_H - 108;
        if (contains(listX, listY, LEFT_W - 20, listH, mouseX, mouseY)) {
            int row = ((int) mouseY - listY) / ROW_H;
            List<BestiaryEntryDto> entries = filteredEntries();
            int index = scroll + row;
            if (index >= 0 && index < entries.size()) {
                selectedEntity = entries.get(index).entityId();
                detailScroll = 0;
                requestSelectedDetails();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listX = panelX() + 10;
        int listY = panelY() + 98;
        int listH = PANEL_H - 108;
        if (contains(listX, listY, LEFT_W - 20, listH, mouseX, mouseY)) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        int detailX = panelX() + LEFT_W;
        int detailY = panelY() + 36;
        int detailW = PANEL_W - LEFT_W - 12;
        int detailH = PANEL_H - 46;
        if (contains(detailX, detailY, detailW, detailH, mouseX, mouseY)) {
            detailScroll = Math.max(0, detailScroll - (int) Math.signum(delta) * 14);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private boolean contains(int x, int y, int w, int h, double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
