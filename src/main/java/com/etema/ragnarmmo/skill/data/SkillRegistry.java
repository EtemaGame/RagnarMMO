package com.etema.ragnarmmo.skill.data;

import com.etema.ragnarmmo.skill.api.SkillCategory;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Runtime registry for skill definitions and effects.
 * Skills are loaded from JSON files by SkillDataLoader.
 */
public final class SkillRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillRegistry.class);
    // Thread-safe maps for runtime access
    private static final Map<ResourceLocation, SkillDefinition> SKILLS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ISkillEffect> EFFECTS = new ConcurrentHashMap<>();

    // Frozen flag to prevent modifications after initial load
    private static volatile boolean frozen = false;

    private SkillRegistry() {
        // Utility class
    }

    // === Skill Definitions ===

    /**
     * Get a skill definition by its ResourceLocation ID.
     *
     * @param id The skill ID (e.g., "ragnarmmo:bash")
     * @return Optional containing the skill definition, or empty if not found
     */
    public static Optional<SkillDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(SKILLS.get(id));
    }

    public static Optional<SkillDefinition> get(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(id))
                .flatMap(SkillRegistry::get);
    }

    /**
     * Get a skill definition, throwing if not found.
     *
     * @param id The skill ID
     * @return The skill definition
     * @throws NoSuchElementException if the skill is not found
     */
    public static SkillDefinition require(ResourceLocation id) {
        return get(id).orElseThrow(() -> new NoSuchElementException("Skill not found: " + id));
    }

    /**
     * Check if a skill is registered.
     *
     * @param id The skill ID
     * @return true if the skill exists
     */
    public static boolean contains(ResourceLocation id) {
        return SKILLS.containsKey(id);
    }

    /**
     * Get all registered skill definitions.
     *
     * @return Unmodifiable collection of all skills
     */
    public static Collection<SkillDefinition> getAll() {
        return Collections.unmodifiableCollection(SKILLS.values());
    }

    /**
     * Get all registered skill IDs.
     *
     * @return Unmodifiable set of all skill IDs
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(SKILLS.keySet());
    }

    /**
     * Get all skills in a specific category.
     *
     * @param category The skill category
     * @return Collection of matching skills
     */
    public static Collection<SkillDefinition> getByCategory(SkillCategory category) {
        return SKILLS.values().stream()
                .filter(skill -> skill.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Get all skills allowed for a specific job.
     *
     * @param jobId The job ID (e.g., "SWORDSMAN")
     * @return Collection of skills allowed for this job
     */
    public static Collection<SkillDefinition> getByJob(String jobId) {
        return SKILLS.values().stream()
                .filter(skill -> skill.getAllowedJobs().isEmpty() || skill.getAllowedJobs().contains(jobId))
                .collect(Collectors.toList());
    }

    /**
     * Get the total count of registered skills.
     *
     * @return Number of registered skills
     */
    public static int size() {
        return SKILLS.size();
    }

    // === Skill Effects ===

    /**
     * Get the effect implementation for a skill.
     *
     * @param skillId The skill ID
     * @return Optional containing the effect, or empty if not found
     */
    public static Optional<ISkillEffect> getEffect(ResourceLocation skillId) {
        return Optional.ofNullable(EFFECTS.get(skillId));
    }

    /**
     * Get the effect implementation for a skill by string ID.
     *
     * @param skillId The skill ID string
     * @return Optional containing the effect, or empty if not found
     */
    public static Optional<ISkillEffect> getEffect(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(skillId))
                .flatMap(SkillRegistry::getEffect);
    }

    /**
     * Get all registered skill effects.
     *
     * @return Unmodifiable collection of all effect implementations
     */
    public static Collection<ISkillEffect> getAllEffects() {
        return Collections.unmodifiableCollection(EFFECTS.values());
    }

    // === Registration (internal use) ===

    /**
     * Register a skill definition.
     * Should only be called by SkillDataLoader during loading.
     *
     * @param definition The skill definition to register
     * @throws IllegalStateException if registry is frozen
     */
    public static void register(SkillDefinition definition) {
        register(definition, false);
    }

    private static void register(SkillDefinition definition, boolean force) {
        if (frozen && !force) {
            LOGGER.warn("Attempted to register skill {} after registry was frozen", definition.getId());
            return;
        }

        ResourceLocation id = definition.getId();
        SkillDefinition existing = SKILLS.put(id, definition);
        if (existing != null && !force) {
            LOGGER.warn("Skill {} was overwritten by a new definition", id);
        }
    }

    /**
     * Register a skill effect.
     * Should only be called by SkillDataLoader during loading.
     *
     * @param skillId The skill ID this effect belongs to
     * @param effect  The effect implementation
     * @throws IllegalStateException if registry is frozen
     */
    public static void registerEffect(ResourceLocation skillId, ISkillEffect effect) {
        if (frozen) {
            LOGGER.warn("Attempted to register effect for {} after registry was frozen", skillId);
            return;
        }

        ISkillEffect existing = EFFECTS.put(skillId, effect);
        if (existing != null) {
            LOGGER.debug("Effect for skill {} was overwritten", skillId);
        }
        
        // Register for event triggers
        com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.registerAll(skillId, effect);
    }

    // === Lifecycle (internal use) ===

    /**
     * Clear all registered skills and effects.
     * Called before reload to prepare for fresh loading.
     */
    public static void clear() {
        frozen = false;
        SKILLS.clear();
        EFFECTS.clear();
        com.etema.ragnarmmo.skill.registry.SkillTriggerRegistry.clear();
        LOGGER.debug("SkillRegistry cleared");
    }

    /**
     * Freeze the registry to prevent further modifications.
     * Called after loading is complete.
     */
    public static void freeze() {
        frozen = true;
        LOGGER.info("SkillRegistry frozen with {} skills and {} effects", SKILLS.size(), EFFECTS.size());
    }

    /**
     * Check if the registry is frozen.
     *
     * @return true if frozen
     */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Apply synchronized definitions from the server.
     * Used only on the client.
     */
    public static void applySync(Collection<SkillDefinition> definitions) {
        LOGGER.info("Applying {} synchronized skill definitions from server", definitions.size());
        
        // Unfreeze to allow updates
        boolean wasFrozen = frozen;
        frozen = false;
        
        // We don't clear() because we want to keep existing effects if any,
        // but we overwrite definitions.
        for (SkillDefinition def : definitions) {
            register(def, true);
        }
        
        frozen = wasFrozen;
        LOGGER.info("SkillRegistry synced and re-frozen");
    }
}
