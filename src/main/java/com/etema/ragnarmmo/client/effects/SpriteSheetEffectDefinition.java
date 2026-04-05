package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

public record SpriteSheetEffectDefinition(
        ResourceLocation id,
        int durationTicks,
        boolean loop,
        ResourceLocation texture,
        int columns,
        int rows,
        int frameCount,
        int framesPerSecond,
        float size,
        EffectOrientation orientation,
        EffectVec3 offset,
        BlendMode blendMode) implements SkillEffectDefinition {

    @Override
    public SkillEffectType type() {
        return SkillEffectType.SPRITE_SHEET;
    }
}
