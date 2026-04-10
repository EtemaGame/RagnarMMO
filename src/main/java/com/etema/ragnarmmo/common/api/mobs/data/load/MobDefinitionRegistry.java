package com.etema.ragnarmmo.common.api.mobs.data.load;

import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobTemplate;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Declarative registry for loaded manual mob datapack resources.
 *
 * <p>This registry stores loaded templates and exact-match definitions only. It is not a runtime authority.</p>
 */
public final class MobDefinitionRegistry {

    private static final MobDefinitionRegistry INSTANCE = new MobDefinitionRegistry();

    private Map<ResourceLocation, MobTemplate> templatesById = Map.of();
    private Map<ResourceLocation, MobDefinition> definitionsById = Map.of();
    private Map<ResourceLocation, MobDefinition> definitionsByEntityTypeId = Map.of();
    private List<MobDefinitionLoadIssue> loadIssues = List.of();

    private MobDefinitionRegistry() {
    }

    public static MobDefinitionRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void replace(
            Map<ResourceLocation, MobTemplate> templatesById,
            Map<ResourceLocation, MobDefinition> definitionsById,
            Map<ResourceLocation, MobDefinition> definitionsByEntityTypeId,
            List<MobDefinitionLoadIssue> loadIssues) {
        this.templatesById = Collections.unmodifiableMap(new LinkedHashMap<>(templatesById));
        this.definitionsById = Collections.unmodifiableMap(new LinkedHashMap<>(definitionsById));
        this.definitionsByEntityTypeId = Collections.unmodifiableMap(new LinkedHashMap<>(definitionsByEntityTypeId));
        this.loadIssues = List.copyOf(loadIssues);
    }

    public synchronized void clear() {
        replace(Map.of(), Map.of(), Map.of(), List.of());
    }

    public Map<ResourceLocation, MobTemplate> getTemplatesById() {
        return templatesById;
    }

    public Map<ResourceLocation, MobDefinition> getDefinitionsById() {
        return definitionsById;
    }

    public Map<ResourceLocation, MobDefinition> getDefinitionsByEntityTypeId() {
        return definitionsByEntityTypeId;
    }

    public List<MobDefinitionLoadIssue> getLoadIssues() {
        return loadIssues;
    }

    public Optional<MobTemplate> getTemplate(ResourceLocation templateId) {
        return Optional.ofNullable(templatesById.get(templateId));
    }

    public Optional<MobDefinition> getDefinition(ResourceLocation entityTypeId) {
        return Optional.ofNullable(definitionsByEntityTypeId.get(entityTypeId));
    }
}
