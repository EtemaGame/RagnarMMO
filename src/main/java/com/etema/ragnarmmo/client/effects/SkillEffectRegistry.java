package com.etema.ragnarmmo.client.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SkillEffectRegistry {
    private static final Map<ResourceLocation, SkillEffectDefinition> REGISTRY = new HashMap<>();

    private SkillEffectRegistry() {
    }

    public static void register(SkillEffectDefinition definition) {
        REGISTRY.put(definition.id(), definition);
    }

    public static Optional<SkillEffectDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
