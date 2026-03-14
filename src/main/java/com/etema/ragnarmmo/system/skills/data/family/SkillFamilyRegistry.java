package com.etema.ragnarmmo.system.skills.data.family;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for skill families.
 * Loaded from data packs at runtime from skill_families directory.
 */
public final class SkillFamilyRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillFamilyRegistry.class);
    private static final Map<ResourceLocation, SkillFamily> FAMILIES = new ConcurrentHashMap<>();
    private static volatile boolean frozen = false;

    private SkillFamilyRegistry() {
    }

    /**
     * Register a skill family.
     *
     * @param family The family to register
     */
    public static void register(SkillFamily family) {
        if (frozen) {
            LOGGER.warn("Attempted to register skill family {} after registry was frozen", family.getId());
            return;
        }

        FAMILIES.put(family.getId(), family);
        LOGGER.debug("Registered skill family: {}", family.getId());
    }

    /**
     * Get a skill family by ID.
     *
     * @param id The family ID
     * @return Optional containing the family if found
     */
    public static Optional<SkillFamily> get(ResourceLocation id) {
        return Optional.ofNullable(FAMILIES.get(id));
    }

    /**
     * Get a skill family by namespace and path.
     *
     * @param namespace The namespace
     * @param path      The path
     * @return Optional containing the family if found
     */
    public static Optional<SkillFamily> get(String namespace, String path) {
        return get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Get all registered skill families.
     *
     * @return Unmodifiable collection of all families
     */
    public static Collection<SkillFamily> getAll() {
        return Collections.unmodifiableCollection(FAMILIES.values());
    }

    /**
     * Get all skill family IDs.
     *
     * @return Unmodifiable set of all family IDs
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(FAMILIES.keySet());
    }

    /**
     * Check if a family is registered.
     *
     * @param id The family ID
     * @return True if registered
     */
    public static boolean contains(ResourceLocation id) {
        return FAMILIES.containsKey(id);
    }

    /**
     * Get the number of registered families.
     *
     * @return Family count
     */
    public static int size() {
        return FAMILIES.size();
    }

    /**
     * Clear all registered families.
     * Called before reload.
     */
    public static void clear() {
        FAMILIES.clear();
        frozen = false;
        LOGGER.info("Cleared skill family registry");
    }

    /**
     * Freeze the registry to prevent further modifications.
     * Called after all families are loaded.
     */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Frozen skill family registry with {} families", FAMILIES.size());
    }

    /**
     * Check if the registry is frozen.
     *
     * @return True if frozen
     */
    public static boolean isFrozen() {
        return frozen;
    }
}
