package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.items.runtime.ZenyWalletHelper;
import com.etema.ragnarmmo.economy.zeny.capability.PlayerWalletProvider;
import com.etema.ragnarmmo.economy.zeny.network.ZenyBagActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

public class MoneyBagScreen extends Screen {

    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_HEIGHT = 180;

    private static final int MARGIN_X = 14;

    private static final int CARD_W = 60;
    private static final int CARD_H = 56;
    private static final int CARD_Y = 72;

    private static final int CARD_1_X = MARGIN_X;
    private static final int CARD_2_X = MARGIN_X + CARD_W + 14;
    private static final int CARD_3_X = MARGIN_X + (CARD_W * 2) + 28;

    private static final int DEPOSIT_X = MARGIN_X;
    private static final int DEPOSIT_Y = 138;
    private static final int DEPOSIT_W = PANEL_WIDTH - (MARGIN_X * 2);
    private static final int DEPOSIT_H = 20;

    // Colors
    private static final int COLOR_BG = 0xF011141A;
    private static final int COLOR_BG_INNER = 0xCC0B0F14;
    private static final int COLOR_BORDER = 0xFF5D6A78;
    private static final int COLOR_BORDER_SOFT = 0xFF37424D;
    private static final int COLOR_BORDER_HOVER = 0xFFE4B54A;
    private static final int COLOR_TEXT = 0xFFE7E7E7;
    private static final int COLOR_MUTED = 0xFF97A0AA;
    private static final int COLOR_ACCENT = 0xFFE4B54A;
    private static final int COLOR_POSITIVE = 0xFF66E26F;
    private static final int COLOR_DISABLED = 0xFF6B6B6B;

    private int panelX;
    private int panelY;
    private float uiScale = 1.0f;

    public MoneyBagScreen() {
        super(Component.translatable("screen.ragnarmmo.money_bag.title"));
    }

    @Override
    protected void init() {
        super.init();
        recalcPanelTransform();
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

    private long getBalance() {
        if (Minecraft.getInstance().player == null) {
            return 0L;
        }

        return PlayerWalletProvider.get(Minecraft.getInstance().player)
                .map(w -> w.getZeny())
                .orElse(0L);
    }

    private long getInventoryValue() {
        if (Minecraft.getInstance().player == null) {
            return 0L;
        }

        long invValue = 0L;
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().items) {
            if (ZenyWalletHelper.isZeny(stack)) {
                invValue += (long) ZenyWalletHelper.getValue(stack) * stack.getCount();
            }
        }
        return invValue;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);

        long balance = getBalance();
        long golds = balance / ZenyWalletHelper.GOLD_VALUE;
        long silvers = (balance % ZenyWalletHelper.GOLD_VALUE) / ZenyWalletHelper.SILVER_VALUE;
        long coppers = balance % ZenyWalletHelper.SILVER_VALUE;
        long invValue = getInventoryValue();

        g.pose().pushPose();
        g.pose().translate(panelX, panelY, 0);
        g.pose().scale(uiScale, uiScale, 1.0f);

        // Outer panel
        g.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BG);
        g.renderOutline(0, 0, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);
        g.fill(4, 4, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, COLOR_BG_INNER);

        // Title
        g.drawString(this.font, getTitle(), MARGIN_X, 10, COLOR_ACCENT, true);

        // Balance card
        int balanceY = 26;
        int balanceH = 24;
        g.fill(MARGIN_X, balanceY, MARGIN_X + DEPOSIT_W, balanceY + balanceH, 0x22000000);
        g.renderOutline(MARGIN_X, balanceY, DEPOSIT_W, balanceH, COLOR_BORDER_SOFT);
        g.drawString(
                this.font,
                Component.translatable("screen.ragnarmmo.money_bag.balance", balance).withStyle(ChatFormatting.YELLOW),
                MARGIN_X + 6,
                balanceY + 8,
                COLOR_TEXT,
                false
        );

        // Section label
        g.drawString(
                this.font,
                Component.translatable("screen.ragnarmmo.money_bag.withdraw").withStyle(ChatFormatting.GRAY),
                MARGIN_X,
                60,
                COLOR_MUTED,
                false
        );

        // Withdraw cards (Order: Copper, Silver, Gold)
        drawWithdrawButton(g, CARD_1_X, CARD_Y, CARD_W, CARD_H, ZenyItems.COPPER_ZENY.get(), coppers, mouseX, mouseY, "screen.ragnarmmo.money_bag.copper");
        drawWithdrawButton(g, CARD_2_X, CARD_Y, CARD_W, CARD_H, ZenyItems.SILVER_ZENY.get(), silvers, mouseX, mouseY, "screen.ragnarmmo.money_bag.silver");
        drawWithdrawButton(g, CARD_3_X, CARD_Y, CARD_W, CARD_H, ZenyItems.GOLD_ZENY.get(), golds, mouseX, mouseY, "screen.ragnarmmo.money_bag.gold");

        // Deposit button
        boolean canDeposit = invValue > 0;
        boolean hoverDep = canDeposit && isHovered(DEPOSIT_X, DEPOSIT_Y, DEPOSIT_W, DEPOSIT_H, mouseX, mouseY);

        int depFill = canDeposit
                ? (hoverDep ? 0x22384B2B : 0x161B2518)
                : 0x14000000;

        int depBorder = canDeposit
                ? (hoverDep ? COLOR_BORDER_HOVER : 0xFF47515C)
                : 0xFF3F3F3F;

        g.fill(DEPOSIT_X, DEPOSIT_Y, DEPOSIT_X + DEPOSIT_W, DEPOSIT_Y + DEPOSIT_H, depFill);
        g.renderOutline(DEPOSIT_X, DEPOSIT_Y, DEPOSIT_W, DEPOSIT_H, depBorder);

        g.drawCenteredString(
                this.font,
                Component.translatable("screen.ragnarmmo.money_bag.deposit_all"),
                PANEL_WIDTH / 2,
                DEPOSIT_Y + 6,
                canDeposit ? COLOR_POSITIVE : COLOR_DISABLED
        );

        // Wait to draw the inventory value slightly lower so it isn't clipped
        g.drawCenteredString(
                this.font,
                Component.translatable("screen.ragnarmmo.money_bag.equivalent_title", invValue).withStyle(ChatFormatting.DARK_GRAY),
                PANEL_WIDTH / 2,
                DEPOSIT_Y + 26,
                COLOR_MUTED
        );

        g.pose().popPose();

        super.render(g, mouseX, mouseY, partialTicks);
    }

    private void drawWithdrawButton(
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h,
            net.minecraft.world.item.Item item,
            long available,
            int mouseX,
            int mouseY,
            String labelKey
    ) {
        boolean enabled = available > 0;
        boolean hovered = enabled && isHovered(x, y, w, h, mouseX, mouseY);

        int bg = enabled
                ? (hovered ? 0x22313F4D : 0x16161F27)
                : 0x12000000;

        int border = enabled
                ? (hovered ? COLOR_BORDER_HOVER : 0xFF586470)
                : 0xFF3F3F3F;

        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, border);

        if (hovered) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, COLOR_ACCENT);
        }

        g.renderItem(new ItemStack(item), x + (w - 16) / 2, y + 6);

        g.drawCenteredString(
                this.font,
                Component.translatable(labelKey),
                x + w / 2,
                y + 27,
                enabled ? COLOR_MUTED : COLOR_DISABLED
        );

        g.drawCenteredString(
                this.font,
                Component.literal("x" + available),
                x + w / 2,
                y + 41,
                enabled ? COLOR_TEXT : COLOR_DISABLED
        );
    }

    private boolean isHovered(int x, int y, int w, int h, int mouseX, int mouseY) {
        double mx = (mouseX - panelX) / uiScale;
        double my = (mouseY - panelY) / uiScale;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        long balance = getBalance();
        long golds = balance / ZenyWalletHelper.GOLD_VALUE;
        long silvers = (balance % ZenyWalletHelper.GOLD_VALUE) / ZenyWalletHelper.SILVER_VALUE;
        long coppers = balance % ZenyWalletHelper.SILVER_VALUE;
        long invValue = getInventoryValue();

        if (invValue > 0 && isHovered(DEPOSIT_X, DEPOSIT_Y, DEPOSIT_W, DEPOSIT_H, (int) mouseX, (int) mouseY)) {
            Network.sendToServer(new ZenyBagActionPacket(ZenyBagActionPacket.Action.DEPOSIT_ALL));
            playClickSound();
            return true;
        }

        if (coppers > 0 && isHovered(CARD_1_X, CARD_Y, CARD_W, CARD_H, (int) mouseX, (int) mouseY)) {
            Network.sendToServer(new ZenyBagActionPacket(ZenyBagActionPacket.Action.WITHDRAW_COPPER));
            playClickSound();
            return true;
        }

        if (silvers > 0 && isHovered(CARD_2_X, CARD_Y, CARD_W, CARD_H, (int) mouseX, (int) mouseY)) {
            Network.sendToServer(new ZenyBagActionPacket(ZenyBagActionPacket.Action.WITHDRAW_SILVER));
            playClickSound();
            return true;
        }

        if (golds > 0 && isHovered(CARD_3_X, CARD_Y, CARD_W, CARD_H, (int) mouseX, (int) mouseY)) {
            Network.sendToServer(new ZenyBagActionPacket(ZenyBagActionPacket.Action.WITHDRAW_GOLD));
            playClickSound();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
