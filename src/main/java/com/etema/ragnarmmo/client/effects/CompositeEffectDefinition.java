package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record CompositeEffectDefinition(
        ResourceLocation id,
        int durationTicks,
        boolean loop,
        List<ChildDefinition> children) implements SkillEffectDefinition {

    @Override
    public SkillEffectType type() {
        return SkillEffectType.COMPOSITE;
    }

    public record ChildDefinition(
            ResourceLocation effectId,
            int startTick,
            EffectVec3 offset,
            float scaleMultiplier,
            EffectColor tint) {
    }
}
