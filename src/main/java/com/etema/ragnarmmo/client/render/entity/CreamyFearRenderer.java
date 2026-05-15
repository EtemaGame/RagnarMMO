package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.CreamyFearModel;
import com.etema.ragnarmmo.entity.mob.CreamyFearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class CreamyFearRenderer extends ConfiguredMobRenderer<CreamyFearEntity> {
    public CreamyFearRenderer(EntityRendererProvider.Context context) {
        super(context, new CreamyFearModel(), 0.7F, 0.45F);
    }
}
