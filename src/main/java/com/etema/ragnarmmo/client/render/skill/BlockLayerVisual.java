package com.etema.ragnarmmo.client.render.skill;

import net.minecraft.resources.ResourceLocation;

/**
 * Declarative block layer used by custom projectile renderers.
 */
public record BlockLayerVisual(
        ResourceLocation blockId,
        float scaleX,
        float scaleY,
        float scaleZ,
        float rotationX,
        float rotationY,
        float rotationZ) {
}
