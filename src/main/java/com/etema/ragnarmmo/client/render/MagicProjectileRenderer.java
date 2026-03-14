package com.etema.ragnarmmo.client.render;

import com.etema.ragnarmmo.system.skills.entity.MagicProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {
    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectileEntity entity) {
        return null; // Invisible entity, only particles show up
    }
}
