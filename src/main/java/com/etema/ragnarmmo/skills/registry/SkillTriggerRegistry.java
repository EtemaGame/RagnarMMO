package com.etema.ragnarmmo.skills.registry;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized registry that maps event triggers to the skills that respond to them.
 * This prevents iterating over all registered skills during every event.
 */
public final class SkillTriggerRegistry {
    private static final Map<ISkillEffect.TriggerType, Set<ResourceLocation>> REGISTRY = new ConcurrentHashMap<>();

    static {
        for (ISkillEffect.TriggerType type : ISkillEffect.TriggerType.values()) {
            REGISTRY.put(type, ConcurrentHashMap.newKeySet());
        }
    }

    private SkillTriggerRegistry() {}

    /**
     * Registers a skill ID for a specific trigger type.
     */
    public static void register(ISkillEffect.TriggerType type, ResourceLocation skillId) {
        REGISTRY.get(type).add(skillId);
    }

    /**
     * Returns an unmodifiable set of skill IDs that respond to the given trigger.
     */
    public static Set<ResourceLocation> getSkillsForTrigger(ISkillEffect.TriggerType type) {
        return Collections.unmodifiableSet(REGISTRY.get(type));
    }

    /**
     * Clears the registry. Called during skill reload.
     */
    public static void clear() {
        REGISTRY.values().forEach(Set::clear);
    }

    /**
     * Helper to register all triggers supported by an effect.
     */
    public static void registerAll(ResourceLocation skillId, ISkillEffect effect) {
        for (ISkillEffect.TriggerType trigger : effect.getSupportedTriggers()) {
            register(trigger, skillId);
        }
    }
}
