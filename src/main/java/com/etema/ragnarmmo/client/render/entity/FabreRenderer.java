package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.FabreModel;
import com.etema.ragnarmmo.entity.mob.FabreEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class FabreRenderer extends ConfiguredMobRenderer<FabreEntity> {
    public FabreRenderer(EntityRendererProvider.Context context) {
        super(context, new FabreModel(), 0.70F, 0.22F);
    }
}
