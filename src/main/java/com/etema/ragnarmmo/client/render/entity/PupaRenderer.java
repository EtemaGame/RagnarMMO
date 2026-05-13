package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.PupaModel;
import com.etema.ragnarmmo.entity.mob.PupaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class PupaRenderer extends ConfiguredMobRenderer<PupaEntity> {
    public PupaRenderer(EntityRendererProvider.Context context) {
        super(context, new PupaModel(), 0.50F, 0.22F);
    }
}
