package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.roitems.network.CardCompoundPacket;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CardCompoundScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 220;

    private int panelX;
    private int panelY;
    private final int cardSlotIndex;
    private final ItemStack cardStack;

    private float uiScale = 1.0f;

    // Track slots that can accept this card
    private static class SlotData {
        int invIndex;
        ItemStack stack;
        int x, y, w, h;
    }

    private final List<SlotData> eligibleSlots = new ArrayList<>();
    private List<Component> deferredTooltip = null;

    public CardCompoundScreen(int cardSlotIndex, ItemStack cardStack) {
        super(Component.translatable("screen.ragnarmmo.card_compound.title"));
        this.cardSlotIndex = cardSlotIndex;
        this.cardStack = cardStack;
    }

    @Override
    protected void init() {
        super.init();
        recalcPanelTransform();
        populateEligibleSlots();
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        recalcPanelTransform();
        populateEligibleSlots();
    }

    private void recalcPanelTransform() {
        float sx = (float) this.width / (float) PANEL_WIDTH;
        float sy = (float) this.height / (float) PANEL_HEIGHT;
        this.uiScale = Math.min(1.0f, Math.min(sx, sy));

        int scaledW = Math.round(PANEL_WIDTH * uiScale);
        int scaledH = Math.round(PANEL_HEIGHT * uiScale);

        this.panelX = (this.width - scaledW) / 2;
        this.panelY = (this.height - scaledH) / 2;
    }

    private void populateEligibleSlots() {
        eligibleSlots.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        int capacity = player.getInventory().getContainerSize();
        int itemsPerRow = 8;
        int startX = 20;
        int startY = 45;
        int spacingX = 28;
        int spacingY = 28;

        int drawnCount = 0;

        for (int i = 0; i < capacity; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty())
                continue;

            int maxSlots = RoItemRuleResolver.resolve(stack).cardSlots();
            if (maxSlots > 0) {
                int currentSlots = RoItemNbtHelper.getSlottedCards(stack).size();
                if (currentSlots < maxSlots) {
                    SlotData data = new SlotData();
                    data.invIndex = i;
                    data.stack = stack;
                    data.x = startX + (drawnCount % itemsPerRow) * spacingX;
                    data.y = startY + (drawnCount / itemsPerRow) * spacingY;
                    data.w = 18;
                    data.h = 18;
                    eligibleSlots.add(data);
                    drawnCount++;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        this.deferredTooltip = null;

        double mx = (mouseX - panelX) / uiScale;
        double my = (mouseY - panelY) / uiScale;

        g.pose().pushPose();
        g.pose().translate(panelX, panelY, 0);
        g.pose().scale(uiScale, uiScale, 1.0f);

        // Background
        g.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BG);
        g.renderOutline(0, 0, PANEL_WIDTH, PANEL_HEIGHT, GuiConstants.COLOR_PANEL_BORDER);

        // Title
        g.drawString(this.font, Component.translatable("screen.ragnarmmo.card_compound.title"), 16, 12, 0xFFFFAA00,
                true);

        // Card Name
        Component cardName = cardStack.getHoverName();
        g.drawString(this.font, Component.translatable("screen.ragnarmmo.card_compound.desc", cardName), 16, 26,
                0xFFDDDDDD, false);

        if (eligibleSlots.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("screen.ragnarmmo.card_compound.no_slots"),
                    PANEL_WIDTH / 2, PANEL_HEIGHT / 2, 0xFFFF5555);
        } else {
            for (SlotData slot : eligibleSlots) {
                boolean hovered = mx >= slot.x && mx < slot.x + slot.w && my >= slot.y && my < slot.y + slot.h;

                // Draw slot BG
                int slotColor = hovered ? 0x88FFFFFF : 0x44000000;
                g.fill(slot.x - 1, slot.y - 1, slot.x + slot.w + 1, slot.y + slot.h + 1, slotColor);
                g.renderOutline(slot.x - 1, slot.y - 1, slot.w + 2, slot.h + 2, 0xFF555555);

                // Draw Item
                g.renderItem(slot.stack, slot.x + 1, slot.y + 1);

                if (hovered) {
                    this.deferredTooltip = this.getTooltipFromItem(Minecraft.getInstance(), slot.stack);
                }
            }
        }

        g.pose().popPose();

        if (deferredTooltip != null) {
            g.renderComponentTooltip(this.font, deferredTooltip, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            double mx = (mouseX - panelX) / uiScale;
            double my = (mouseY - panelY) / uiScale;

            for (SlotData slot : eligibleSlots) {
                if (mx >= slot.x && mx < slot.x + slot.w && my >= slot.y && my < slot.y + slot.h) {
                    Network.sendToServer(new CardCompoundPacket(cardSlotIndex, slot.invIndex));
                    Minecraft.getInstance().setScreen(null); // Close screen
                    Minecraft.getInstance().getSoundManager()
                            .play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                                    .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
