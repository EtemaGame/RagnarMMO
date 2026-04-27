package com.etema.ragnarmmo.skills.api;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry to decouple skill IDs from their implementation classes.
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
     * Creates an effect instance from a registered effect ID.
     */
    public static Optional<ISkillEffect> create(String effectIdentifier, ResourceLocation skillId) {
        Function<ResourceLocation, ISkillEffect> factory = REGISTRY.get(effectIdentifier);
        try {
            if (factory != null) {
                return Optional.of(factory.apply(skillId));
            }
            return createFromClassName(effectIdentifier, skillId);
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate effect for {}: {}", skillId, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<ISkillEffect> createFromClassName(String className, ResourceLocation skillId) throws Exception {
        Class<?> type = Class.forName(className);
        if (!ISkillEffect.class.isAssignableFrom(type)) {
            LOGGER.warn("Effect class '{}' for skill {} does not implement ISkillEffect", className, skillId);
            return Optional.empty();
        }
        try {
            var constructor = type.getDeclaredConstructor(ResourceLocation.class);
            constructor.setAccessible(true);
            return Optional.of((ISkillEffect) constructor.newInstance(skillId));
        } catch (NoSuchMethodException ignored) {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return Optional.of((ISkillEffect) constructor.newInstance());
        }
    }
}
