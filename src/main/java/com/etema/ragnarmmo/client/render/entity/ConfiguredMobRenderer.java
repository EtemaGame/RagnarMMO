package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.entity.mob.AbstractRagnarMobEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ConfiguredMobRenderer<T extends AbstractRagnarMobEntity> extends GeoEntityRenderer<T> {
    public ConfiguredMobRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float scale, float shadowRadius) {
        super(context, model);
        this.withScale(scale);
        this.shadowRadius = shadowRadius;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
