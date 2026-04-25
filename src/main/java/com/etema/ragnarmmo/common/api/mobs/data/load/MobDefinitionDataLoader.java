package com.etema.ragnarmmo.common.api.mobs.data.load;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobDirectStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobRoStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobTemplate;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionIssue;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolutionResult;
import com.etema.ragnarmmo.common.api.mobs.data.resolve.MobDefinitionResolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Datapack loader for authored mob templates and definitions.
 *
 * <p>This loader is limited to declarative resource parsing, exact-match registry population, and
 * load-time diagnostics. It does not perform runtime integration.</p>
 */
public final class MobDefinitionDataLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String DEFINITIONS_FOLDER = "mob_definitions";
    private static final String TEMPLATES_FOLDER = "mob_templates";

    private static final Set<String> DEFINITION_FIELDS = Set.of(
            "entity", "template", "rank", "level", "ro_stats", "direct_stats", "race", "element", "size");
    private static final Set<String> TEMPLATE_FIELDS = Set.of(
            "rank", "level", "ro_stats", "direct_stats", "race", "element", "size");
    private static final Set<String> RO_STATS_FIELDS = Set.of("str", "agi", "vit", "int", "dex", "luk");
    private static final Set<String> DIRECT_STATS_FIELDS = Set.of(
            "max_hp", "atk_min", "atk_max", "def", "mdef", "hit", "flee", "crit", "aspd", "move_speed");

    public static final MobDefinitionDataLoader INSTANCE = new MobDefinitionDataLoader();

    private MobDefinitionDataLoader() {
        super(GSON, DEFINITIONS_FOLDER);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> definitionResources,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        MobDefinitionRegistry registry = MobDefinitionRegistry.getInstance();

        Map<ResourceLocation, MobTemplate> templatesById = new LinkedHashMap<>();
        Map<ResourceLocation, MobDefinition> definitionsById = new LinkedHashMap<>();
        Map<ResourceLocation, MobDefinition> definitionsByEntityTypeId = new LinkedHashMap<>();
        List<MobDefinitionLoadIssue> issues = new ArrayList<>();
        Map<ResourceLocation, ResourceLocation> definitionSourceByEntityTypeId = new LinkedHashMap<>();
        Set<ResourceLocation> duplicateEntityTypeIds = new HashSet<>();

        loadTemplates(resourceManager, templatesById, issues);

        for (Map.Entry<ResourceLocation, JsonElement> entry : definitionResources.entrySet()) {
            ResourceLocation sourceId = entry.getKey();
            JsonElement element = entry.getValue();

            try {
                if (!element.isJsonObject()) {
                    issues.add(new MobDefinitionLoadIssue(
                            MobDefinitionLoadIssue.Kind.INVALID,
                            sourceId,
                            null,
                            "definition resource must be a JSON object"));
                    continue;
                }

                MobDefinition definition = parseDefinition(sourceId, element.getAsJsonObject());
                MobTemplate template = resolveTemplate(definition, templatesById, sourceId, issues);
                if (definition.template() != null && template == null) {
                    continue;
                }

                MobDefinitionResolutionResult resolution = MobDefinitionResolver.resolve(definition, template);
                if (!resolution.issues().isEmpty()) {
                    for (MobDefinitionResolutionIssue issue : resolution.issues()) {
                        issues.add(new MobDefinitionLoadIssue(
                                mapResolutionKind(issue.kind()),
                                sourceId,
                                definition.entity(),
                                issue.message()));
                    }
                    continue;
                }

                ResourceLocation entityTypeId = definition.entity();
                if (duplicateEntityTypeIds.contains(entityTypeId)) {
                    issues.add(new MobDefinitionLoadIssue(
                            MobDefinitionLoadIssue.Kind.DUPLICATE,
                            sourceId,
                            entityTypeId,
                            "duplicate exact-match definition for entity_type id " + entityTypeId));
                    continue;
                }

                ResourceLocation existingSource = definitionSourceByEntityTypeId.get(entityTypeId);
                if (existingSource != null) {
                    duplicateEntityTypeIds.add(entityTypeId);
                    definitionSourceByEntityTypeId.remove(entityTypeId);
                    definitionsByEntityTypeId.remove(entityTypeId);
                    definitionsById.remove(existingSource);

                    issues.add(new MobDefinitionLoadIssue(
                            MobDefinitionLoadIssue.Kind.DUPLICATE,
                            existingSource,
                            entityTypeId,
                            "duplicate exact-match definition for entity_type id " + entityTypeId));
                    issues.add(new MobDefinitionLoadIssue(
                            MobDefinitionLoadIssue.Kind.DUPLICATE,
                            sourceId,
                            entityTypeId,
                            "duplicate exact-match definition for entity_type id " + entityTypeId));
                    continue;
                }

                definitionSourceByEntityTypeId.put(entityTypeId, sourceId);
                definitionsById.put(sourceId, definition);
                definitionsByEntityTypeId.put(entityTypeId, definition);
            } catch (Exception ex) {
                issues.add(new MobDefinitionLoadIssue(
                        MobDefinitionLoadIssue.Kind.INVALID,
                        sourceId,
                        null,
                        ex.getMessage()));
            }
        }

        registry.replace(templatesById, definitionsById, definitionsByEntityTypeId, issues);

        RagnarMMO.LOGGER.info(
                "Loaded {} mob templates, {} mob definitions, {} load issues",
                templatesById.size(),
                definitionsByEntityTypeId.size(),
                issues.size());
    }

    private static void loadTemplates(
            ResourceManager resourceManager,
            Map<ResourceLocation, MobTemplate> templatesById,
            List<MobDefinitionLoadIssue> issues) {
        Map<ResourceLocation, Resource> templateResources = resourceManager.listResources(
                TEMPLATES_FOLDER,
                location -> location.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : templateResources.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();
            ResourceLocation templateId = toDataId(fileLocation, TEMPLATES_FOLDER);

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element == null || !element.isJsonObject()) {
                    issues.add(new MobDefinitionLoadIssue(
                            MobDefinitionLoadIssue.Kind.INVALID,
                            templateId,
                            null,
                            "template resource must be a JSON object"));
                    continue;
                }

                MobTemplate template = parseTemplate(templateId, element.getAsJsonObject());
                templatesById.put(templateId, template);
            } catch (Exception ex) {
                issues.add(new MobDefinitionLoadIssue(
                        MobDefinitionLoadIssue.Kind.INVALID,
                        templateId,
                        null,
                        ex.getMessage()));
            }
        }
    }

    private static @Nullable MobTemplate resolveTemplate(
            MobDefinition definition,
            Map<ResourceLocation, MobTemplate> templatesById,
            ResourceLocation sourceId,
            List<MobDefinitionLoadIssue> issues) {
        if (definition.template() == null) {
            return null;
        }
        MobTemplate template = templatesById.get(definition.template());
        if (template == null) {
            issues.add(new MobDefinitionLoadIssue(
                    MobDefinitionLoadIssue.Kind.INVALID,
                    sourceId,
                    definition.entity(),
                    "referenced template does not exist: " + definition.template()));
        }
        return template;
    }

    private static MobTemplate parseTemplate(ResourceLocation sourceId, JsonObject json) {
        validateAllowedFields(sourceId, json, TEMPLATE_FIELDS, "template");

        return new MobTemplate(
                parseOptionalRank(sourceId, json, "rank"),
                parseOptionalInteger(sourceId, json, "level"),
                parseRoStatsBlock(sourceId, json),
                parseDirectStatsBlock(sourceId, json),
                parseOptionalString(sourceId, json, "race"),
                parseOptionalString(sourceId, json, "element"),
                parseOptionalString(sourceId, json, "size"));
    }

    private static MobDefinition parseDefinition(ResourceLocation sourceId, JsonObject json) {
        validateAllowedFields(sourceId, json, DEFINITION_FIELDS, "definition");

        ResourceLocation entity = parseRequiredResourceLocation(sourceId, json, "entity");
        ResourceLocation template = parseOptionalResourceLocation(sourceId, json, "template");

        return new MobDefinition(
                entity,
                template,
                parseOptionalRank(sourceId, json, "rank"),
                parseOptionalInteger(sourceId, json, "level"),
                parseRoStatsBlock(sourceId, json),
                parseDirectStatsBlock(sourceId, json),
                parseOptionalString(sourceId, json, "race"),
                parseOptionalString(sourceId, json, "element"),
                parseOptionalString(sourceId, json, "size"));
    }

    private static @Nullable MobRoStatsBlock parseRoStatsBlock(ResourceLocation sourceId, JsonObject json) {
        if (!json.has("ro_stats")) {
            return null;
        }
        JsonElement element = json.get("ro_stats");
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("ro_stats must be a JSON object in " + sourceId);
        }

        JsonObject block = element.getAsJsonObject();
        validateAllowedFields(sourceId, block, RO_STATS_FIELDS, "ro_stats");
        return new MobRoStatsBlock(
                parseOptionalInteger(sourceId, block, "str"),
                parseOptionalInteger(sourceId, block, "agi"),
                parseOptionalInteger(sourceId, block, "vit"),
                parseOptionalInteger(sourceId, block, "int"),
                parseOptionalInteger(sourceId, block, "dex"),
                parseOptionalInteger(sourceId, block, "luk"));
    }

    private static @Nullable MobDirectStatsBlock parseDirectStatsBlock(ResourceLocation sourceId, JsonObject json) {
        if (!json.has("direct_stats")) {
            return null;
        }
        JsonElement element = json.get("direct_stats");
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("direct_stats must be a JSON object in " + sourceId);
        }

        JsonObject block = element.getAsJsonObject();
        validateAllowedFields(sourceId, block, DIRECT_STATS_FIELDS, "direct_stats");
        return new MobDirectStatsBlock(
                parseOptionalInteger(sourceId, block, "max_hp"),
                parseOptionalInteger(sourceId, block, "atk_min"),
                parseOptionalInteger(sourceId, block, "atk_max"),
                parseOptionalInteger(sourceId, block, "def"),
                parseOptionalInteger(sourceId, block, "mdef"),
                parseOptionalInteger(sourceId, block, "hit"),
                parseOptionalInteger(sourceId, block, "flee"),
                parseOptionalInteger(sourceId, block, "crit"),
                parseOptionalInteger(sourceId, block, "aspd"),
                parseOptionalDouble(sourceId, block, "move_speed"));
    }

    private static void validateAllowedFields(
            ResourceLocation sourceId,
            JsonObject json,
            Set<String> allowedFields,
            String context) {
        for (String field : json.keySet()) {
            if (!allowedFields.contains(field)) {
                throw new IllegalArgumentException(
                        "unknown field '" + field + "' in " + context + " for " + sourceId);
            }
        }
    }

    private static @Nullable String parseOptionalString(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        if (!json.has(field)) {
            return null;
        }
        JsonElement element = json.get(field);
        if (element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException(field + " must be a string in " + sourceId);
        }
        return element.getAsString();
    }

    private static @Nullable Integer parseOptionalInteger(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        if (!json.has(field)) {
            return null;
        }
        JsonElement element = json.get(field);
        if (element.isJsonNull() || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + " must be a number in " + sourceId);
        }
        return element.getAsInt();
    }

    private static @Nullable Double parseOptionalDouble(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        if (!json.has(field)) {
            return null;
        }
        JsonElement element = json.get(field);
        if (element.isJsonNull() || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + " must be a number in " + sourceId);
        }
        return element.getAsDouble();
    }

    private static @Nullable ResourceLocation parseOptionalResourceLocation(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        String raw = parseOptionalString(sourceId, json, field);
        if (raw == null) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(field + " must be a valid resource location in " + sourceId);
        }
        return parsed;
    }

    private static ResourceLocation parseRequiredResourceLocation(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        ResourceLocation parsed = parseOptionalResourceLocation(sourceId, json, field);
        if (parsed == null) {
            throw new IllegalArgumentException(field + " is required in " + sourceId);
        }
        return parsed;
    }

    private static @Nullable MobRank parseOptionalRank(
            ResourceLocation sourceId,
            JsonObject json,
            String field) {
        String raw = parseOptionalString(sourceId, json, field);
        if (raw == null) {
            return null;
        }
        try {
            return MobRank.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " must be one of NORMAL, ELITE, BOSS in " + sourceId);
        }
    }

    private static ResourceLocation toDataId(ResourceLocation fileLocation, String rootFolder) {
        String prefix = rootFolder + "/";
        String path = fileLocation.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("resource is outside " + rootFolder + ": " + fileLocation);
        }
        String dataPath = path.substring(prefix.length(), path.length() - ".json".length());
        return new ResourceLocation(fileLocation.getNamespace(), dataPath);
    }

    private static MobDefinitionLoadIssue.Kind mapResolutionKind(MobDefinitionResolutionIssue.Kind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case INVALID -> MobDefinitionLoadIssue.Kind.INVALID;
            case INCOMPLETE -> MobDefinitionLoadIssue.Kind.INCOMPLETE;
        };
    }

    @Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(INSTANCE);
        }
    }
}
