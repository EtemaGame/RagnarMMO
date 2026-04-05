package com.etema.ragnarmmo.client.effects.render;

import com.etema.ragnarmmo.client.effects.BlendMode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class EffectRenderTypes {
    private EffectRenderTypes() {
    }

    public static RenderType resolve(BlendMode blendMode, ResourceLocation texture) {
        return switch (blendMode) {
            case CUTOUT -> RenderType.entityCutoutNoCull(texture);
            case EMISSIVE, ADDITIVE -> RenderType.entityTranslucentEmissive(texture);
            case TRANSLUCENT -> RenderType.entityTranslucent(texture);
        };
    }
}
