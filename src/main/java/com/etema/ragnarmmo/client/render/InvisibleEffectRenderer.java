package com.etema.ragnarmmo.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Renderer for marker/effect entities whose visuals are drawn elsewhere.
 */
public class InvisibleEffectRenderer<T extends Entity> extends EntityRenderer<T> {
    public InvisibleEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
