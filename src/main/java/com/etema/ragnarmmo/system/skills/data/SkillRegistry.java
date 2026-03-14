package com.etema.ragnarmmo.system.skills.data;

import com.etema.ragnarmmo.common.api.skills.ISkillDefinition;
import com.etema.ragnarmmo.common.api.skills.SkillCategory;
import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Runtime registry for skill definitions and effects.
 * Skills are loaded from JSON files by SkillDataLoader.
 * This registry supports /reload for hot-reloading skill definitions.
 */
public final class SkillRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillRegistry.class);
    private static final String DEFAULT_NAMESPACE = "ragnarmmo";

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

    /**
     * Get a skill definition by its legacy string ID.
     * Supports both full IDs ("ragnarmmo:bash") and legacy IDs ("bash").
     *
     * @param id The skill ID string
     * @return Optional containing the skill definition, or empty if not found
     */
    public static Optional<SkillDefinition> get(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }

        // If it contains ":", parse as ResourceLocation
        if (id.contains(":")) {
            return get(new ResourceLocation(id));
        }

        // Legacy format: assume default namespace
        return get(new ResourceLocation(DEFAULT_NAMESPACE, id.toLowerCase(Locale.ROOT)));
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
        if (skillId.contains(":")) {
            return getEffect(new ResourceLocation(skillId));
        }
        return getEffect(new ResourceLocation(DEFAULT_NAMESPACE, skillId.toLowerCase(Locale.ROOT)));
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
        if (frozen) {
            LOGGER.warn("Attempted to register skill {} after registry was frozen", definition.getId());
            return;
        }

        ResourceLocation id = definition.getId();
        SkillDefinition existing = SKILLS.put(id, definition);
        if (existing != null) {
            LOGGER.warn("Skill {} was overwritten by a new definition", id);
        } else {
            LOGGER.debug("Registered skill: {}", id);
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
     * Get the default namespace used for legacy IDs.
     *
     * @return The default namespace ("ragnarmmo")
     */
    public static String getDefaultNamespace() {
        return DEFAULT_NAMESPACE;
    }
}
