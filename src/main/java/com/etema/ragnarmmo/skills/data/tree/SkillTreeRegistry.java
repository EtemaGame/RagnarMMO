package com.etema.ragnarmmo.skills.data.tree;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for skill tree layouts.
 * Loaded from data packs at runtime from skill_trees directory.
 */
public final class SkillTreeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillTreeRegistry.class);
    private static final Map<ResourceLocation, SkillTreeDefinition> TREES = new ConcurrentHashMap<>();
    private static volatile boolean frozen = false;

    private SkillTreeRegistry() {
    }

    /**
     * Register a skill tree definition.
     *
     * @param tree The tree to register
     */
    public static void register(SkillTreeDefinition tree) {
        if (frozen) {
            LOGGER.warn("Attempted to register skill tree {} after registry was frozen", tree.getId());
            return;
        }

        TREES.put(tree.getId(), tree);
        LOGGER.debug("Registered skill tree: {}", tree.getId());
    }

    /**
     * Get a skill tree by ID.
     *
     * @param id The tree ID
     * @return Optional containing the tree if found
     */
    public static Optional<SkillTreeDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(TREES.get(id));
    }

    /**
     * Get a skill tree by namespace and path.
     *
     * @param namespace The namespace
     * @param path      The path
     * @return Optional containing the tree if found
     */
    public static Optional<SkillTreeDefinition> get(String namespace, String path) {
        return get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Get skill tree for a specific job and tier.
     *
     * @param job  Job name (e.g., "MAGE")
     * @param tier Tier number (1 for first job, 2 for second job)
     * @return Optional containing the tree if found
     */
    public static Optional<SkillTreeDefinition> getForJob(String job, int tier) {
        String treeName = job.toLowerCase() + "_" + tier;
        return get("ragnarmmo", treeName);
    }

    /**
     * Get all registered skill trees.
     *
     * @return Unmodifiable collection of all trees
     */
    public static Collection<SkillTreeDefinition> getAll() {
        return Collections.unmodifiableCollection(TREES.values());
    }

    /**
     * Get all skill tree IDs.
     *
     * @return Unmodifiable set of all tree IDs
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(TREES.keySet());
    }

    /**
     * Check if a tree is registered.
     *
     * @param id The tree ID
     * @return True if registered
     */
    public static boolean contains(ResourceLocation id) {
        return TREES.containsKey(id);
    }

    /**
     * Get the number of registered trees.
     *
     * @return Tree count
     */
    public static int size() {
        return TREES.size();
    }

    /**
     * Clear all registered trees.
     * Called before reload.
     */
    public static void clear() {
        TREES.clear();
        frozen = false;
        LOGGER.info("Cleared skill tree registry");
    }

    /**
     * Freeze the registry to prevent further modifications.
     * Called after all trees are loaded.
     */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Frozen skill tree registry with {} trees", TREES.size());
    }

    /**
     * Check if the registry is frozen.
     *
     * @return True if frozen
     */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Apply synchronized definitions from server.
     * Used only on client.
     */
    public static void applySync(Collection<SkillTreeDefinition> trees) {
        LOGGER.info("Applying {} synchronized skill tree layouts from server", trees.size());
        
        // Unfreeze to allow updates
        boolean wasFrozen = frozen;
        frozen = false;
        
        for (SkillTreeDefinition tree : trees) {
            register(tree);
        }
        
        frozen = wasFrozen;
        LOGGER.info("SkillTreeRegistry synced and re-frozen");
    }
}
