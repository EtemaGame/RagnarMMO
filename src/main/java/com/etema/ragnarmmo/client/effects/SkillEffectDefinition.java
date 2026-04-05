package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

public interface SkillEffectDefinition {
    ResourceLocation id();

    SkillEffectType type();

    int durationTicks();

    default boolean loop() {
        return false;
    }
}
