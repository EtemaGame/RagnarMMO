package com.etema.ragnarmmo.skill.registry;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry to decouple Skill IDs from their implementation classes.
 * Instead of specifying FQN in JSON, we specify an ID that this factory resolves.
 */
public final class SkillEffectFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillEffectFactory.class);
    private static final Map<String, Function<ResourceLocation, ISkillEffect>> REGISTRY = new ConcurrentHashMap<>();

    private SkillEffectFactory() {}

    /**
     * Registers a factory function for a specific effect ID.
     */
    public static void register(String effectId, Function<ResourceLocation, ISkillEffect> factory) {
        REGISTRY.put(effectId, factory);
    }

    /**
     * Creates an effect instance based on an effect ID or class FQN (fallback).
     */
    public static Optional<ISkillEffect> create(String effectIdentifier, ResourceLocation skillId) {
        // 1. Try resolving via registered short name/ID
        if (REGISTRY.containsKey(effectIdentifier)) {
            return Optional.of(REGISTRY.get(effectIdentifier).apply(skillId));
        }

        // 2. Fallback: try legacy reflection based on FQN
        try {
            Class<?> clazz = Class.forName(effectIdentifier);
            if (ISkillEffect.class.isAssignableFrom(clazz)) {
                try {
                    // New pattern: ResourceLocation constructor
                    return Optional.of((ISkillEffect) clazz
                            .getDeclaredConstructor(ResourceLocation.class).newInstance(skillId));
                } catch (NoSuchMethodException e) {
                    // Legacy pattern: no-arg constructor
                    return Optional.of((ISkillEffect) clazz
                            .getDeclaredConstructor().newInstance());
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Identifier is likely a short name not yet registered
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate effect for {}: {}", skillId, e.getMessage());
        }

        return Optional.empty();
    }
}
