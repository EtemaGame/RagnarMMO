package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record LayeredEffectDefinition(
        ResourceLocation id,
        int durationTicks,
        boolean loop,
        List<LayerDefinition> layers) implements SkillEffectDefinition {

    @Override
    public SkillEffectType type() {
        return SkillEffectType.LAYERED;
    }

    public record LayerDefinition(
            String name,
            BlendMode blendMode,
            EffectOrientation orientation,
            int zOrder,
            List<KeyframeDefinition> keyframes) {
    }

    public record KeyframeDefinition(
            int tick,
            float x,
            float y,
            float z,
            float scaleX,
            float scaleY,
            float rotationDeg,
            float alpha,
            float r,
            float g,
            float b,
            ResourceLocation texture) {

        public EffectColor color() {
            return new EffectColor(r, g, b, alpha);
        }
    }
}
