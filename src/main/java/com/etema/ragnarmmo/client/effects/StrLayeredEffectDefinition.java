package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StrLayeredEffectDefinition(
        ResourceLocation id,
        LayeredEffectDefinition layeredDefinition,
        List<String> warnings) implements SkillEffectDefinition {

    @Override
    public SkillEffectType type() {
        return SkillEffectType.STR_LAYERED;
    }

    @Override
    public int durationTicks() {
        return layeredDefinition.durationTicks();
    }

    @Override
    public boolean loop() {
        return layeredDefinition.loop();
    }
}
