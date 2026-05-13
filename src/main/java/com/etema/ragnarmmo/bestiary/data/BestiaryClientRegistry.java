package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDetailsDto;
import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDto;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BestiaryClientRegistry {
    private static int version;
    private static boolean loaded;
    private static List<BestiaryEntryDto> entries = List.of();
    private static final Map<ResourceLocation, BestiaryEntryDetailsDto> DETAILS = new LinkedHashMap<>();

    private BestiaryClientRegistry() {
    }

    public static synchronized void replace(int newVersion, List<BestiaryEntryDto> newEntries) {
        version = newVersion;
        loaded = true;
        entries = List.copyOf(newEntries);
        DETAILS.keySet().removeIf(id -> entries.stream().noneMatch(entry -> entry.entityId().equals(id)));
    }

    public static synchronized void putDetails(BestiaryEntryDetailsDto details) {
        DETAILS.put(details.entityId(), details);
    }

    public static synchronized Optional<BestiaryEntryDetailsDto> details(ResourceLocation entityId) {
        return Optional.ofNullable(DETAILS.get(entityId));
    }

    public static synchronized List<BestiaryEntryDto> entries() {
        return entries;
    }

    public static synchronized int version() {
        return version;
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized boolean isEmpty() {
        return entries.isEmpty();
    }
}
