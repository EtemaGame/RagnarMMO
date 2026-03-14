package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public class HudOverlayConfigScreen extends Screen {
    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 400;

    private final List<DraggableComponent> components = new ArrayList<>();
    private DraggableComponent selectedComponent;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    // UI Controls
    private RangeSlider scaleSlider;
    private RangeSlider alphaSlider;
    private ToggleCheckbox showBgCheckbox;

    public HudOverlayConfigScreen() {
        super(Component.translatable("screen.ragnarmmo.overlay.title"));
    }

    @Override
    protected void init() {
        super.init();

        components.clear();
        addComponent(RagnarConfigs.CLIENT.hud.status, "Status", RagnarStatusOverlay::getStatusHeight,
                RagnarStatusOverlay::renderStatus);

        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomY = this.height - buttonHeight - 20;

        this.addRenderableWidget(
                Button.builder(Component.translatable("screen.ragnarmmo.overlay.button.done"), btn -> this.onClose())
                        .bounds(this.width / 2 - buttonWidth / 2, bottomY, buttonWidth, buttonHeight)
                        .build());

        // Sliders & Controls layout
        int controlWidth = 120;
        int controlX = 10;
        int controlY = this.height / 2 - 40;

        // Scale Slider
        scaleSlider = new RangeSlider(
                controlX, controlY, controlWidth, 20,
                Component.literal("Scale: "),
                0.1, 3.0, 1.0, 0.1, true,
                val -> {
                    if (selectedComponent != null) {
                        selectedComponent.setScale(val);
                    }
                });
        this.addRenderableWidget(scaleSlider);

        // Alpha Slider
        alphaSlider = new RangeSlider(
                controlX, controlY + 25, controlWidth, 20,
                Component.literal("Alpha: "),
                0, 255, 100, 5, false,
                val -> {
                    if (selectedComponent != null) {
                        selectedComponent.setAlpha(val.intValue());
                    }
                });
        this.addRenderableWidget(alphaSlider);

        // Background Checkbox
        showBgCheckbox = new ToggleCheckbox(
                controlX, controlY + 50, controlWidth, 20,
                Component.literal("Show Background"),
                true,
                val -> {
                    if (selectedComponent != null) {
                        selectedComponent.setShowBackground(val);
                    }
                });
        this.addRenderableWidget(showBgCheckbox);

        updateControls();
    }

    // Helper to render hotbar preview
    private void renderHotbarPreview(GuiGraphics guiGraphics, Font font, int startIndex, int count) {
        int SLOT_SIZE = 20;
        int SLOT_SPACING = 2;

        for (int i = 0; i < count; i++) {
            int x = i * (SLOT_SIZE + SLOT_SPACING);
            guiGraphics.renderOutline(x, 0, SLOT_SIZE, SLOT_SIZE, 0x4DFFFFFF);
            guiGraphics.drawString(font, String.valueOf(startIndex + i + 1), x + 1, 1, 0xFFE0E0E0, true);
        }
    }

    private void addComponent(RagnarConfigs.Client.Hud.HudComponent config, String name,
            ToIntFunction<Font> heightProvider, RenderConsumer renderer) {
        if (config.enabled.get()) {
            components.add(new DraggableComponent(config, name, heightProvider, renderer));
        }
    }

    private void updateControls() {
        if (selectedComponent != null) {
            scaleSlider.active = true;
            alphaSlider.active = true;
            showBgCheckbox.active = true;

            scaleSlider.setValue(selectedComponent.getScale());
            alphaSlider.setValue((double) selectedComponent.getAlpha());

            showBgCheckbox.setSelected(selectedComponent.getShowBackground());
        } else {
            scaleSlider.active = false;
            alphaSlider.active = false;
            showBgCheckbox.active = false;
            showBgCheckbox.setSelected(false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        graphics.drawCenteredString(this.font, Component.translatable("screen.ragnarmmo.overlay.instructions.move"),
                this.width / 2, 20, 0xFFFFFF);

        if (selectedComponent != null) {
            graphics.drawString(this.font, "Selected: " + selectedComponent.name, 10, this.height / 2 - 60, 0xFFFFFF,
                    false);
        } else {
            graphics.drawString(this.font, "Select a component to edit", 10, this.height / 2 - 60, 0xAAAAAA, false);
        }

        if (player != null) {
            RagnarCoreAPI.get(player).ifPresent(stats -> {
                for (DraggableComponent comp : components) {
                    comp.render(graphics, this.font, stats, player, this.width, this.height);

                    // Highlight selected or hovered
                    if (comp == selectedComponent
                            || comp.isMouseOver(mouseX, mouseY, this.font, this.width, this.height)) {
                        comp.renderSelection(graphics, this.font, this.width, this.height);
                    }
                }
            });
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean clickedComponent = false;
            // Reverse iteration (top-most first)
            for (int i = components.size() - 1; i >= 0; i--) {
                DraggableComponent comp = components.get(i);
                if (comp.isMouseOver(mouseX, mouseY, this.font, this.width, this.height)) {
                    selectedComponent = comp;
                    dragging = true;

                    int[] pos = comp.getPosition(this.font, this.width, this.height);
                    dragOffsetX = (int) (mouseX - pos[0]);
                    dragOffsetY = (int) (mouseY - pos[1]);

                    clickedComponent = true;
                    updateControls();
                    return true;
                }
            }

            // Check widgets (sliders/checkbox)
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            if (!clickedComponent) {
                selectedComponent = null;
                updateControls();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && selectedComponent != null && dragging) {
            int newX = (int) Math.round(mouseX) - dragOffsetX;
            int newY = (int) Math.round(mouseY) - dragOffsetY;
            selectedComponent.updateAnchor(newX, newY, this.font, this.width, this.height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        for (DraggableComponent comp : components) {
            comp.save();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private interface RenderConsumer {
        int render(GuiGraphics g, Font f, IPlayerStats s, Player p, int w);
    }

    private class DraggableComponent {
        final RagnarConfigs.Client.Hud.HudComponent config;
        final String name;
        final ToIntFunction<Font> heightProvider;
        final RenderConsumer renderer;

        double anchorX, anchorY;
        double scale;
        int alpha;
        boolean showBg;

        DraggableComponent(RagnarConfigs.Client.Hud.HudComponent config, String name,
                ToIntFunction<Font> heightProvider, RenderConsumer renderer) {
            this.config = config;
            this.name = name;
            this.heightProvider = heightProvider;
            this.renderer = renderer;
            this.anchorX = Mth.clamp(config.anchorX.get(), 0.0, 1.0);
            this.anchorY = Mth.clamp(config.anchorY.get(), 0.0, 1.0);
            this.scale = config.scale.get();
            this.alpha = config.backgroundAlpha.get();
            this.showBg = config.showBackground.get();
        }

        int[] getPosition(Font font, int screenWidth, int screenHeight) {
            int width = RagnarConfigs.CLIENT.hud.width.get();
            int realWidth = (int) (width * scale);

            int x = (int) Math.round(anchorX * Math.max(0, screenWidth - realWidth));
            int y = (int) Math.round(anchorY * Math.max(0, screenHeight - 0));
            return new int[] { x, y };
        }

        void render(GuiGraphics graphics, Font font, IPlayerStats stats, Player player,
                int screenWidth, int screenHeight) {
            int width = RagnarConfigs.CLIENT.hud.width.get();
            int[] pos = getPosition(font, screenWidth, screenHeight);

            // Preview logic: calculate dimensions
            // We use the renderer to get height if possible, or provider
            int height = heightProvider.applyAsInt(font);

            int realW = (int) (width * scale);
            int realH = (int) (height * scale);

            // Draw BG
            if (showBg && alpha > 0) {
                int bgColor = (alpha << 24) | 0x000000;
                graphics.fill(pos[0] - 2, pos[1] - 2, pos[0] + realW + 2, pos[1] + realH + 2, bgColor);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(pos[0], pos[1], 0);
            graphics.pose().scale((float) scale, (float) scale, 1.0f);

            renderer.render(graphics, font, stats, player, width);

            graphics.pose().popPose();
        }

        void renderSelection(GuiGraphics graphics, Font font, int screenWidth,
                int screenHeight) {
            int width = RagnarConfigs.CLIENT.hud.width.get();
            int[] pos = getPosition(font, screenWidth, screenHeight);
            int h = heightProvider.applyAsInt(font);
            int realW = (int) (width * scale);
            int realH = (int) (h * scale);

            graphics.fillGradient(pos[0] - 2, pos[1] - 2, pos[0] + realW + 2, pos[1] + realH + 2, 0x40FFFFFF,
                    0x40FFFFFF);
            graphics.renderOutline(pos[0] - 2, pos[1] - 2, realW + 4, realH + 4, 0xFFFFFFFF);
        }

        boolean isMouseOver(double mouseX, double mouseY, Font font, int screenWidth,
                int screenHeight) {
            int width = RagnarConfigs.CLIENT.hud.width.get();
            int[] pos = getPosition(font, screenWidth, screenHeight);
            int h = heightProvider.applyAsInt(font);
            int realW = (int) (width * scale);
            int realH = (int) (h * scale);

            return mouseX >= pos[0] - 2 && mouseX <= pos[0] + realW + 2 &&
                    mouseY >= pos[1] - 2 && mouseY <= pos[1] + realH + 2;
        }

        void updateAnchor(int pixelX, int pixelY, Font font, int screenWidth,
                int screenHeight) {
            int width = RagnarConfigs.CLIENT.hud.width.get();
            int realWidth = (int) (width * scale);

            int maxX = Math.max(0, screenWidth - realWidth);
            int maxY = Math.max(0, screenHeight - 0);

            int clampedX = Mth.clamp(pixelX, 0, maxX);
            int clampedY = Mth.clamp(pixelY, 0, maxY);

            this.anchorX = maxX == 0 ? 0.0 : (double) clampedX / (double) maxX;
            this.anchorY = maxY == 0 ? 0.0 : (double) clampedY / (double) maxY;
        }

        void setScale(double s) {
            this.scale = s;
        }

        double getScale() {
            return scale;
        }

        void setAlpha(int a) {
            this.alpha = a;
        }

        int getAlpha() {
            return alpha;
        }

        void setShowBackground(boolean s) {
            this.showBg = s;
        }

        boolean getShowBackground() {
            return showBg;
        }

        void save() {
            config.anchorX.set(anchorX);
            config.anchorY.set(anchorY);
            config.scale.set(scale);
            config.backgroundAlpha.set(alpha);
            config.showBackground.set(showBg);
        }
    }

    private static class RangeSlider extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;
        private final double stepSize;
        private final Component prefix;
        private final Consumer<Double> onChange;
        private final boolean showDecimals;

        public RangeSlider(int x, int y, int width, int height, Component prefix, double minValue, double maxValue,
                double currentValue, double stepSize, boolean showDecimals,
                Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), 0);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.stepSize = stepSize;
            this.prefix = prefix;
            this.onChange = onChange;
            this.showDecimals = showDecimals;
            this.value = (Math.max(minValue, Math.min(maxValue, currentValue)) - minValue) / (maxValue - minValue);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double currentVal = getValue();
            String valStr;
            if (showDecimals) {
                valStr = String.format("%.1f", currentVal);
            } else {
                valStr = String.valueOf((int) Math.round(currentVal));
            }
            this.setMessage(this.prefix.copy().append(valStr));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getValue());
        }

        public void setValue(double val) {
            this.value = (Math.max(minValue, Math.min(maxValue, val)) - minValue) / (maxValue - minValue);
            this.updateMessage();
        }

        private double getValue() {
            double val = minValue + this.value * (maxValue - minValue);
            if (stepSize > 0) {
                val = Math.round(val / stepSize) * stepSize;
            }
            return Math.max(minValue, Math.min(maxValue, val));
        }
    }

    private static class ToggleCheckbox extends net.minecraft.client.gui.components.AbstractButton {
        private boolean selected;
        private final Consumer<Boolean> onChange;

        public ToggleCheckbox(int x, int y, int width, int height, Component message, boolean selected,
                Consumer<Boolean> onChange) {
            super(x, y, width, height, message);
            this.selected = selected;
            this.onChange = onChange;
        }

        @Override
        public void onPress() {
            this.selected = !this.selected;
            this.onChange.accept(this.selected);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isSelected() {
            return this.selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            int color = this.active ? 0xFFFFFF : 0xA0A0A0;

            // Draw checkbox box
            int boxSize = 11;
            int boxX = this.getX();
            int boxY = this.getY() + (this.height - boxSize) / 2;

            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF000000);
            graphics.renderOutline(boxX, boxY, boxSize, boxSize, 0xFFAAAAAA);

            if (this.selected) {
                graphics.drawString(mc.font, "\u2714", boxX + 2, boxY + 1, 0xFF00FF00, false);
            }

            // Draw label
            graphics.drawString(mc.font, this.getMessage(), boxX + boxSize + 4,
                    this.getY() + (this.height - 8) / 2, color, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
