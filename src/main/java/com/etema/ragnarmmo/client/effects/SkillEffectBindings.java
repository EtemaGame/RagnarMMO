package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SkillEffectBindings(
        ResourceLocation skillId,
        List<BindingEntry> bindings) {

    public record BindingEntry(
            EffectTriggerPhase phase,
            ResourceLocation effectId) {
    }
}
