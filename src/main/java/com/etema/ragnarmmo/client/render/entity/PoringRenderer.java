package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.PoringModel;
import com.etema.ragnarmmo.entity.mob.PoringEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PoringRenderer extends GeoEntityRenderer<PoringEntity> {
    public PoringRenderer(EntityRendererProvider.Context context) {
        super(context, new PoringModel());
        this.withScale(0.70F);
        this.shadowRadius = 0.22F;
    }

    @Override
    public RenderType getRenderType(PoringEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
