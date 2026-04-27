package com.etema.ragnarmmo.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ClientCardTooltip implements ClientTooltipComponent {

    private final ResourceLocation mobTexture;
    private final Component descriptionComponent;

    // Fixed image size we will render in the tooltip
    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;
    private static final int PADDING = 4;

    // The base background for the card inside the tooltip (optional, using default
    // tooltip background for now)

    public ClientCardTooltip(CardTooltipData data) {
        String mobId = data.mobId();
        String path = mobId.contains(":") ? mobId.split(":")[1] : mobId;
        String namespace = mobId.contains(":") ? mobId.split(":")[0] : "minecraft";

        // This expects a texture at
        // assets/ragnarmmo/textures/gui/cards/<namespace>/<mobName>.png
        this.mobTexture = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "textures/gui/cards/" + namespace + "/" + path + ".png");

        // If translation key is empty, don't show desc
        this.descriptionComponent = data.descriptionKey() != null && !data.descriptionKey().isEmpty()
                ? Component.translatable(data.descriptionKey())
                : Component.empty();
    }

    @Override
    public int getHeight() {
        // Height needs space for the image + some padding
        return IMAGE_HEIGHT + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        // We'll put the image on the left, and word-wrap the description on the right.
        // For simplicity, let's fix a max width for the description.
        int descWidth = 120;
        return IMAGE_WIDTH + PADDING * 3 + descWidth;
    }

    @Override
    public void renderImage(Font font, int startX, int startY, GuiGraphics graphics) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Render the Mob Image
        graphics.blit(this.mobTexture, startX + PADDING, startY + PADDING, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH,
                IMAGE_HEIGHT);

        // Render the text next to the image
        int textX = startX + IMAGE_WIDTH + PADDING * 2;
        int textY = startY + PADDING;

        // Draw word-wrapped description
        if (!this.descriptionComponent.getString().isEmpty()) {
            graphics.drawWordWrap(font, this.descriptionComponent, textX, textY, 120, 0xAAAAAA);
        }

        RenderSystem.disableBlend();
    }
}
