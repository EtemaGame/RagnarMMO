package com.etema.ragnarmmo.client.render.skill;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side registry for skill visuals.
 */
public class SkillVisualsRegistry {
    private static final Map<ResourceLocation, SkillVisuals> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation skillId, SkillVisuals visuals) {
        REGISTRY.put(skillId, visuals);
    }

    public static Optional<SkillVisuals> get(ResourceLocation skillId) {
        return Optional.ofNullable(REGISTRY.get(skillId));
    }
}
