package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.PoringModel;
import com.etema.ragnarmmo.entity.mob.PoringEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class DropRenderer extends ConfiguredMobRenderer<PoringEntity> {
    public DropRenderer(EntityRendererProvider.Context context) {
        super(context, new PoringModel(), 0.70F, 0.22F);
    }
}
