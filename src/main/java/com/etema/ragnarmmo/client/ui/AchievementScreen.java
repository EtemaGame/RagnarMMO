package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.system.achievements.data.AchievementCategory;
import com.etema.ragnarmmo.system.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.system.achievements.data.AchievementRegistry;
import com.etema.ragnarmmo.system.achievements.network.ClaimAchievementPacket;
import com.etema.ragnarmmo.system.achievements.network.SetTitlePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AchievementScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(RagnarMMO.MODID, "textures/gui/achievement_screen.png");

    private final int imageWidth = 256;
    private final int imageHeight = 200;
    private int leftPos;
    private int topPos;

    private AchievementCategory selectedCategory = AchievementCategory.BASIC;
    private List<AchievementDefinition> filteredAchievements;

    private int scrollOffset = 0;
    private final int ITEM_HEIGHT = 40;
    private final int VISIBLE_ITEMS = 4;

    public AchievementScreen() {
        super(Component.literal("Achievements"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        refreshList();

        // Category Tabs
        int tabWidth = 40;
        int tabX = leftPos + 8;
        int tabY = topPos + 6;

        for (AchievementCategory category : AchievementCategory.values()) {
            this.addRenderableWidget(Button.builder(Component.literal(category.name()), b -> {
                this.selectedCategory = category;
                this.scrollOffset = 0;
                refreshList();
            }).bounds(tabX, tabY, tabWidth, 16).build());
            tabX += tabWidth + 2;
        }

        // Remove active title button
        this.addRenderableWidget(Button.builder(Component.literal("Clear Title"), b -> {
            Network.sendToServer(new SetTitlePacket(""));
        }).bounds(leftPos + imageWidth - 70, topPos + imageHeight - 20, 65, 16).build());
    }

    private void refreshList() {
        this.filteredAchievements = AchievementRegistry.getInstance().getAll().values().stream()
                .filter(a -> a.category() == selectedCategory)
                .sorted(Comparator.comparing(AchievementDefinition::id))
                .collect(Collectors.toList());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Render fallback background using a rect if texture doesn't exist
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF333333);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(this.font, "Achievements - " + selectedCategory.name(), leftPos + 8, topPos + 26, 0xFFFFFF, false);

        if (Minecraft.getInstance().player == null)
            return;

        Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            g.drawString(this.font, "Points: " + cap.getTotalPoints(), leftPos + imageWidth - 60, topPos + 26, 0xFFFF55,
                    false);

            int startY = topPos + 40;
            for (int i = 0; i < VISIBLE_ITEMS; i++) {
                int index = scrollOffset + i;
                if (index >= filteredAchievements.size())
                    break;

                AchievementDefinition def = filteredAchievements.get(index);
                renderAchievementItem(g, def, cap, leftPos + 8, startY + (i * ITEM_HEIGHT), mouseX, mouseY);
            }
        });
    }

    private void renderAchievementItem(GuiGraphics g, AchievementDefinition def,
            com.etema.ragnarmmo.system.achievements.capability.IPlayerAchievements cap, int x, int y, int mouseX,
            int mouseY) {
        boolean unlocked = cap.isUnlocked(def.id());
        boolean claimed = cap.isClaimed(def.id());

        int bgColor = unlocked ? (claimed ? 0xFF225522 : 0xFF555522) : 0xFF222222;
        g.fill(x, y, x + imageWidth - 16, y + ITEM_HEIGHT - 2, bgColor);
        g.renderOutline(x, y, imageWidth - 16, ITEM_HEIGHT - 2, 0xFFAAAAAA);

        g.drawString(this.font, Component.translatable(def.name()), x + 5, y + 5, unlocked ? 0xFFFF55 : 0xAAAAAA,
                false);
        g.drawString(this.font, Component.translatable(def.description()), x + 5, y + 15, 0xDDDDDD, false);

        if (def.title() != null && !def.title().isEmpty()) {
            g.drawString(this.font, "Title: [" + def.title() + "]", x + 5, y + 25, 0x55FFFF, false);
        }

        // Draw progress or status
        if (!unlocked && !def.triggerType().equals("level_up")) {
            int progress = cap.getProgress(def.id() + "_progress");
            g.drawString(this.font, progress + " / " + def.requiredAmount(), x + imageWidth - 60, y + ITEM_HEIGHT - 12,
                    0xAAAAAA, false);
        } else if (unlocked && !claimed) {
            // Drawn via mouseClicked right now due to lazy button impl
            g.drawString(this.font, "> Claim! <", x + imageWidth - 60, y + ITEM_HEIGHT - 12, 0x55FF55, false);
        } else if (claimed) {
            g.drawString(this.font, "Claimed", x + imageWidth - 60, y + ITEM_HEIGHT - 12, 0x5555FF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS)
                    .ifPresent(cap -> {
                        int startY = topPos + 40;
                        for (int i = 0; i < VISIBLE_ITEMS; i++) {
                            int index = scrollOffset + i;
                            if (index >= filteredAchievements.size())
                                break;

                            int x = leftPos + 8;
                            int y = startY + (i * ITEM_HEIGHT);

                            if (mouseX >= x && mouseX <= x + imageWidth - 16 && mouseY >= y
                                    && mouseY <= y + ITEM_HEIGHT - 2) {
                                AchievementDefinition def = filteredAchievements.get(index);

                                if (cap.isUnlocked(def.id())) {
                                    if (!cap.isClaimed(def.id())) {
                                        Network.sendToServer(new ClaimAchievementPacket(def.id()));
                                    } else if (def.title() != null) {
                                        Network.sendToServer(new SetTitlePacket(def.title()));
                                    }
                                }
                            }
                        }
                    });
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0) {
            int maxOffset = Math.max(0, filteredAchievements.size() - VISIBLE_ITEMS);
            scrollOffset = Math.min(scrollOffset + 1, maxOffset);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
