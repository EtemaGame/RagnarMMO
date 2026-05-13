package com.etema.ragnarmmo.client.render.entity;

import com.etema.ragnarmmo.client.model.entity.LunaticModel;
import com.etema.ragnarmmo.entity.mob.LunaticEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class LunaticRenderer extends ConfiguredMobRenderer<LunaticEntity> {
    public LunaticRenderer(EntityRendererProvider.Context context) {
        super(context, new LunaticModel(), 0.9F, 0.40F);
    }
}
