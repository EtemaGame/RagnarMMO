package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.CreamyModel;
import com.etema.ragnarmmo.entity.mob.CreamyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class CreamyRenderer extends ConfiguredMobRenderer<CreamyEntity> {
    public CreamyRenderer(EntityRendererProvider.Context context) {
        super(context, new CreamyModel(), 0.7F, 0.45F);
    }
}
