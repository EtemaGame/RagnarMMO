package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.bestiary.api.BestiaryCategory;
import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDto;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class BestiaryIndexBuilder {
    private BestiaryIndexBuilder() {
    }

    public static List<BestiaryEntryDto> build(Map<ResourceLocation, BestiaryOverride> overrides) {
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .map(type -> toEntry(type, overrides))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(entry -> entry.entityId().toString()))
                .toList();
    }

    private static java.util.Optional<BestiaryEntryDto> toEntry(
            EntityType<?> type,
            Map<ResourceLocation, BestiaryOverride> overrides) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (entityId == null) {
            RagnarMMO.LOGGER.debug("Skipping bestiary entity with null registry key: {}", type);
            return java.util.Optional.empty();
        }

        BestiaryOverride override = overrides.get(entityId);
        if (override != null && !override.visible()) {
            return java.util.Optional.empty();
        }

        if (override == null && !BestiaryClassificationService.isSupportedCategory(type.getCategory())) {
            return java.util.Optional.empty();
        }

        BestiaryCategory category = override != null && override.category().isPresent()
                ? override.category().get()
                : BestiaryClassificationService.classify(entityId, type);
        String descriptionId = override != null ? override.descriptionId() : "";
        return java.util.Optional.of(new BestiaryEntryDto(entityId, entityId.getNamespace(), category, descriptionId));
    }
}
