package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.MukaModel;
import com.etema.ragnarmmo.entity.mob.MukaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class MukaRenderer extends ConfiguredMobRenderer<MukaEntity> {
    public MukaRenderer(EntityRendererProvider.Context context) {
        super(context, new MukaModel(), 0.50F, 0.22F);
    }
}
