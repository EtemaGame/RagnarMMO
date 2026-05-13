package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.client.PartyHudOverlay;
import com.etema.ragnarmmo.client.SkillOverlay;
import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
import com.etema.ragnarmmo.client.hud.HudWidgetDefinition;
import com.etema.ragnarmmo.client.hud.HudWidgetState;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public class HudOverlayConfigScreen extends Screen {
    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 400;
    private static final int SNAP_DISTANCE = 6;

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
        addComponent(RagnarConfigs.CLIENT.hud.status,
                Component.translatable("screen.ragnarmmo.overlay.widget.status"),
                font -> Mth.clamp(RagnarConfigs.CLIENT.hud.width.get(), MIN_WIDTH, MAX_WIDTH),
                RagnarStatusOverlay::getStatusHeight,
                RagnarStatusOverlay::renderStatus);
        addComponent(RagnarConfigs.CLIENT.hud.cast,
                Component.translatable("screen.ragnarmmo.overlay.widget.cast"),
                font -> CastOverlay.getCastWidth(),
                CastOverlay::getCastHeight,
                (g, f, s, p, w) -> CastOverlay.renderPreview(g, f, w));
        addComponent(RagnarConfigs.CLIENT.hud.skillHotbar,
                Component.translatable("screen.ragnarmmo.overlay.widget.skill_hotbar"),
                font -> HotbarOverlay.getWidth(),
                font -> HotbarOverlay.getHeight(),
                (g, f, s, p, w) -> {
                    HotbarOverlay.renderPreview(g, 0, 6);
                    return HotbarOverlay.getHeight();
                });
        addComponent(RagnarConfigs.CLIENT.hud.partyFrame,
                Component.translatable("screen.ragnarmmo.overlay.widget.party_frame"),
                font -> PartyHudOverlay.getWidth(),
                PartyHudOverlay::getPreviewHeight,
                (g, f, s, p, w) -> PartyHudOverlay.renderPreview(g, f));
        addComponent(RagnarConfigs.CLIENT.hud.notifications,
                Component.translatable("screen.ragnarmmo.overlay.widget.notifications"),
                font -> SkillOverlay.getWidth(),
                font -> SkillOverlay.getHeight(),
                (g, f, s, p, w) -> SkillOverlay.renderPreview(g, f));
        components.sort(Comparator.comparingInt(component -> component.zOrder));

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
                Component.translatable("screen.ragnarmmo.overlay.control.scale"),
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
                Component.translatable("screen.ragnarmmo.overlay.control.alpha"),
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
                Component.translatable("screen.ragnarmmo.overlay.control.show_background"),
                true,
                val -> {
                    if (selectedComponent != null) {
                        selectedComponent.setShowBackground(val);
                    }
                });
        this.addRenderableWidget(showBgCheckbox);

        updateControls();
    }

    private void addComponent(RagnarConfigs.Client.Hud.HudComponent config, Component name,
            ToIntFunction<Font> widthProvider, ToIntFunction<Font> heightProvider, RenderConsumer renderer) {
        if (config.enabled.get()) {
            components.add(new DraggableComponent(config,
                    new HudWidgetDefinition(configName(config), name, config.zOrder.get()),
                    widthProvider, heightProvider, renderer));
        }
    }

    private static String configName(RagnarConfigs.Client.Hud.HudComponent config) {
        return "hud_widget_" + Integer.toHexString(System.identityHashCode(config));
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
            graphics.drawString(this.font,
                    Component.translatable("screen.ragnarmmo.overlay.selected", selectedComponent.definition.displayName()),
                    10, this.height / 2 - 60, 0xFFFFFF, false);
        } else {
            graphics.drawString(this.font, Component.translatable("screen.ragnarmmo.overlay.select_component"),
                    10, this.height / 2 - 60, 0xAAAAAA, false);
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
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            // Reverse iteration (top-most first)
            for (int i = components.size() - 1; i >= 0; i--) {
                DraggableComponent comp = components.get(i);
                if (comp.isMouseOver(mouseX, mouseY, this.font, this.width, this.height)) {
                    selectedComponent = comp;
                    dragging = true;

                    int[] pos = comp.getPosition(this.font, this.width, this.height);
                    dragOffsetX = (int) (mouseX - pos[0]);
                    dragOffsetY = (int) (mouseY - pos[1]);

                    updateControls();
                    return true;
                }
            }

            selectedComponent = null;
            updateControls();
            return true;
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
        final HudWidgetDefinition definition;
        final ToIntFunction<Font> widthProvider;
        final ToIntFunction<Font> heightProvider;
        final RenderConsumer renderer;

        double anchorX, anchorY;
        double scale;
        int alpha;
        boolean showBg;
        int zOrder;

        DraggableComponent(RagnarConfigs.Client.Hud.HudComponent config, HudWidgetDefinition definition,
                ToIntFunction<Font> widthProvider, ToIntFunction<Font> heightProvider, RenderConsumer renderer) {
            this.config = config;
            this.definition = definition;
            this.widthProvider = widthProvider;
            this.heightProvider = heightProvider;
            this.renderer = renderer;
            HudWidgetState state = HudConfigSerializer.read(config);
            this.anchorX = Mth.clamp(state.anchorX(), 0.0, 1.0);
            this.anchorY = Mth.clamp(state.anchorY(), 0.0, 1.0);
            this.scale = state.scale();
            this.alpha = state.backgroundAlpha();
            this.showBg = state.showBackground();
            this.zOrder = state.zOrder();
        }

        int[] getPosition(Font font, int screenWidth, int screenHeight) {
            HudLayoutManager.HudBounds bounds = getBounds(font, screenWidth, screenHeight);
            return new int[] { bounds.x(), bounds.y() };
        }

        void render(GuiGraphics graphics, Font font, IPlayerStats stats, Player player,
                int screenWidth, int screenHeight) {
            int width = getWidth(font);
            int height = heightProvider.applyAsInt(font);
            HudLayoutManager.HudBounds bounds = getBounds(font, screenWidth, screenHeight);

            // Draw BG
            HudLayoutManager.renderBackground(graphics, currentState(), bounds);

            HudLayoutManager.pushWidgetTransform(graphics, bounds);
            renderer.render(graphics, font, stats, player, width);
            HudLayoutManager.popWidgetTransform(graphics);
        }

        void renderSelection(GuiGraphics graphics, Font font, int screenWidth,
                int screenHeight) {
            HudLayoutManager.HudBounds bounds = getBounds(font, screenWidth, screenHeight);

            graphics.fillGradient(bounds.x() - 2, bounds.y() - 2,
                    bounds.x() + bounds.realWidth() + 2, bounds.y() + bounds.realHeight() + 2, 0x40FFFFFF,
                    0x40FFFFFF);
            graphics.renderOutline(bounds.x() - 2, bounds.y() - 2,
                    bounds.realWidth() + 4, bounds.realHeight() + 4, 0xFFFFFFFF);
        }

        boolean isMouseOver(double mouseX, double mouseY, Font font, int screenWidth,
                int screenHeight) {
            HudLayoutManager.HudBounds bounds = getBounds(font, screenWidth, screenHeight);
            return mouseX >= bounds.x() - 2 && mouseX <= bounds.x() + bounds.realWidth() + 2 &&
                    mouseY >= bounds.y() - 2 && mouseY <= bounds.y() + bounds.realHeight() + 2;
        }

        void updateAnchor(int pixelX, int pixelY, Font font, int screenWidth,
                int screenHeight) {
            int width = getWidth(font);
            int height = heightProvider.applyAsInt(font);
            int realWidth = Math.max(1, (int) Math.round(width * scale));
            int realHeight = Math.max(1, (int) Math.round(height * scale));
            pixelX = snapCoordinate(pixelX, realWidth, screenWidth);
            pixelY = snapCoordinate(pixelY, realHeight, screenHeight);
            HudLayoutManager.Anchor anchor = HudLayoutManager.anchorFromPixel(
                    pixelX, pixelY, currentState(), width, height, screenWidth, screenHeight);
            this.anchorX = anchor.x();
            this.anchorY = anchor.y();
        }

        void reloadFromConfig() {
            HudWidgetState state = HudConfigSerializer.read(config);
            this.anchorX = Mth.clamp(state.anchorX(), 0.0, 1.0);
            this.anchorY = Mth.clamp(state.anchorY(), 0.0, 1.0);
            this.scale = state.scale();
            this.alpha = state.backgroundAlpha();
            this.showBg = state.showBackground();
            this.zOrder = state.zOrder();
        }

        int getWidth(Font font) {
            return Math.max(1, widthProvider.applyAsInt(font));
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
            HudConfigSerializer.write(config, currentState());
        }

        private HudLayoutManager.HudBounds getBounds(Font font, int screenWidth, int screenHeight) {
            return HudLayoutManager.bounds(
                    currentState(),
                    getWidth(font),
                    heightProvider.applyAsInt(font),
                    screenWidth,
                    screenHeight);
        }

        private HudWidgetState currentState() {
            return new HudWidgetState(config.enabled.get(), anchorX, anchorY, scale, alpha, showBg, zOrder);
        }
    }

    private static int snapCoordinate(int pixel, int widgetSize, int screenSize) {
        int max = Math.max(0, screenSize - widgetSize);
        int center = max / 2;
        if (Math.abs(pixel) <= SNAP_DISTANCE) {
            return 0;
        }
        if (Math.abs(pixel - center) <= SNAP_DISTANCE) {
            return center;
        }
        if (Math.abs(pixel - max) <= SNAP_DISTANCE) {
            return max;
        }
        return Mth.clamp(pixel, 0, max);
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
