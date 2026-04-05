package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillEffectBindingsRegistry {
    private static final Map<ResourceLocation, EnumMap<EffectTriggerPhase, List<ResourceLocation>>> REGISTRY =
            new HashMap<>();

    private SkillEffectBindingsRegistry() {
    }

    public static void register(SkillEffectBindings bindings) {
        EnumMap<EffectTriggerPhase, List<ResourceLocation>> phases =
                REGISTRY.computeIfAbsent(bindings.skillId(), key -> new EnumMap<>(EffectTriggerPhase.class));
        for (SkillEffectBindings.BindingEntry entry : bindings.bindings()) {
            phases.computeIfAbsent(entry.phase(), ignored -> new ArrayList<>()).add(entry.effectId());
        }
    }

    public static List<ResourceLocation> resolve(ResourceLocation skillId, EffectTriggerPhase phase) {
        EnumMap<EffectTriggerPhase, List<ResourceLocation>> byPhase = REGISTRY.get(skillId);
        if (byPhase == null) {
            return List.of();
        }
        return byPhase.getOrDefault(phase, List.of());
    }

    public static List<ResourceLocation> resolveWithFallback(ResourceLocation skillId, EffectTriggerPhase phase) {
        if (skillId == null) {
            return List.of();
        }

        List<ResourceLocation> resolved = new ArrayList<>(resolve(skillId, phase));
        if (resolved.isEmpty() && SkillEffectRegistry.contains(skillId)) {
            resolved.add(skillId);
        }
        return List.copyOf(resolved);
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
