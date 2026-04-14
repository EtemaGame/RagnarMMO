package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ManualMobCatalogService {

    public record Filters(
            String modNamespace,
            Boolean covered,
            String backend,
            Boolean enabled) {
    }

    private ManualMobCatalogService() {
    }

    public static List<ManualMobCatalogEntry> scan(MinecraftServer server, Filters filters) {
        String namespaceFilter = normalize(filters.modNamespace());
        String backendFilter = normalize(filters.backend());

        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(ManualMobCatalogService::isEditableLivingType)
                .map(type -> toCatalogEntry(type, server))
                .filter(entry -> namespaceFilter == null || entry.namespace().equals(namespaceFilter))
                .filter(entry -> filters.covered() == null || (entry.internalPresent() || entry.datapackCoverage()) == filters.covered())
                .filter(entry -> backendFilter == null || entry.effectiveBackend().equals(backendFilter))
                .filter(entry -> filters.enabled() == null || entry.internalEnabled() == filters.enabled())
                .sorted(Comparator.comparing(ManualMobCatalogEntry::entityTypeId))
                .toList();
    }

    public static boolean isEditableLivingType(EntityType<?> type) {
        if (type == EntityType.PLAYER || type == null) {
            return false;
        }
        return LivingEntity.class.isAssignableFrom(type.getBaseClass());
    }

    private static ManualMobCatalogEntry toCatalogEntry(EntityType<?> type, MinecraftServer server) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String idText = entityTypeId == null ? "unknown:unknown" : entityTypeId.toString();
        
        var internalEntryOpt = entityTypeId != null 
                ? ManualMobRegistryService.find(server, entityTypeId) 
                : Optional.<InternalManualMobEntry>empty();
        
        boolean internalPresent = internalEntryOpt.isPresent();
        boolean internalEnabled = internalEntryOpt.map(InternalManualMobEntry::enabled).orElse(false);
        
        boolean datapackCoverage = entityTypeId != null
                && ManualMobProfileResolver.resolve(entityTypeId).profile() != null;
                
        String effectiveBackend = "none";
        boolean manualEffective = false;
        if (entityTypeId != null) {
            var resolved = ManualMobBackendResolver.resolve(entityTypeId, server, MobStatsConfigAccess.getManualMobBackend());
            if (resolved.coverage().covered()) {
                effectiveBackend = resolved.coverage().backend().name().toLowerCase(Locale.ROOT);
                manualEffective = resolved.coverage().backend() == com.etema.ragnarmmo.common.config.RagnarConfigs.ManualMobBackend.INTERNAL;
            }
        }

        return new ManualMobCatalogEntry(
                idText,
                entityTypeId == null ? "unknown" : entityTypeId.getNamespace(),
                type.getDescription().getString(),
                internalPresent,
                internalEnabled,
                datapackCoverage,
                effectiveBackend,
                manualEffective);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
