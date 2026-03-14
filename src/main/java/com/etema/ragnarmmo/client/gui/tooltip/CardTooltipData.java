package com.etema.ragnarmmo.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Data record passed from the server/item stack to the client tooltip renderer.
 */
public record CardTooltipData(
        String cardId,
        String mobId,
        String descriptionKey) implements TooltipComponent {
}
