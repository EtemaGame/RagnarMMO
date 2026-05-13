package com.etema.ragnarmmo.client.model.entity;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.init.RagnarEntities;
import com.etema.ragnarmmo.entity.mob.PoringEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PoringModel extends GeoModel<PoringEntity> {
    @Override
    public ResourceLocation getModelResource(PoringEntity animatable) {
        return id("geo/entity/poring.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PoringEntity animatable) {
        if (animatable.getType() == RagnarEntities.POPORING.get()) {
            return id("textures/entity/poporing.png");
        }
        if (animatable.getType() == RagnarEntities.DROP.get()) {
            return id("textures/entity/drop.png");
        }
        if (animatable.getType() == RagnarEntities.MARIN.get()) {
            return id("textures/entity/marin.png");
        }
        return id("textures/entity/poring.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PoringEntity animatable) {
        return id("animations/entity/poring.animation.json");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, path);
    }
}
