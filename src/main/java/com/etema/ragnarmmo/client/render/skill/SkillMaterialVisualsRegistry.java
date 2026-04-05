package com.etema.ragnarmmo.client.render.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side registry for block-layer based skill visuals.
 */
public final class SkillMaterialVisualsRegistry {
    private static final Map<ResourceLocation, List<BlockLayerVisual>> REGISTRY = new HashMap<>();

    private SkillMaterialVisualsRegistry() {
    }

    public static void register(ResourceLocation skillId, List<BlockLayerVisual> layers) {
        REGISTRY.put(skillId, List.copyOf(layers));
    }

    public static Optional<List<BlockLayerVisual>> get(ResourceLocation skillId) {
        return Optional.ofNullable(REGISTRY.get(skillId));
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
