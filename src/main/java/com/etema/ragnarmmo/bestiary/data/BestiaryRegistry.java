package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDetailsDto;
import com.etema.ragnarmmo.bestiary.api.BestiaryEntryDto;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BestiaryRegistry {
    private static final BestiaryRegistry INSTANCE = new BestiaryRegistry();

    private int version;
    private boolean built;
    private List<BestiaryEntryDto> index = List.of();
    private Map<ResourceLocation, BestiaryEntryDto> visibleById = Map.of();
    private Map<ResourceLocation, BestiaryOverride> overrides = Map.of();
    private List<BestiaryLoadIssue> loadIssues = List.of();

    private BestiaryRegistry() {
    }

    public static BestiaryRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized BestiaryIndexSnapshot currentIndex() {
        if (!built) {
            rebuildIndex();
        }
        return new BestiaryIndexSnapshot(version, index);
    }

    public synchronized Optional<BestiaryEntryDto> visibleEntry(ResourceLocation entityId) {
        if (!built) {
            rebuildIndex();
        }
        return Optional.ofNullable(visibleById.get(entityId));
    }

    public synchronized Optional<BestiaryEntryDetailsDto> details(ResourceLocation entityId, MinecraftServer server) {
        if (!built) {
            rebuildIndex();
        }
        if (!visibleById.containsKey(entityId)) {
            return Optional.empty();
        }
        return Optional.of(BestiaryDetailsResolver.resolve(entityId, overrides.get(entityId), server));
    }

    public synchronized void replaceOverrides(
            Map<ResourceLocation, BestiaryOverride> newOverrides,
            List<BestiaryLoadIssue> issues) {
        overrides = Map.copyOf(newOverrides);
        loadIssues = List.copyOf(issues);
        invalidate();
        RagnarMMO.LOGGER.info("Bestiary metadata loaded: {} overrides, {} issues", overrides.size(), loadIssues.size());
        for (BestiaryLoadIssue issue : loadIssues) {
            RagnarMMO.LOGGER.debug("Bestiary load issue [{}] {} entity={} source={}",
                    issue.kind(), issue.message(), issue.entityId(), issue.source());
        }
    }

    public synchronized void invalidate() {
        built = false;
        version++;
        index = List.of();
        visibleById = Map.of();
    }

    public synchronized void clear() {
        overrides = Map.of();
        loadIssues = List.of();
        invalidate();
    }

    public void syncToPlayer(ServerPlayer player) {
        BestiaryIndexSnapshot snapshot = currentIndex();
        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                new com.etema.ragnarmmo.bestiary.network.SyncBestiaryIndexPacket(snapshot.version(), snapshot.entries()));
    }

    private void rebuildIndex() {
        index = BestiaryIndexBuilder.build(overrides);
        Map<ResourceLocation, BestiaryEntryDto> byId = new LinkedHashMap<>();
        for (BestiaryEntryDto entry : index) {
            byId.put(entry.entityId(), entry);
        }
        visibleById = Map.copyOf(byId);
        built = true;
    }

    public record BestiaryIndexSnapshot(int version, List<BestiaryEntryDto> entries) {
        public BestiaryIndexSnapshot {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }
}
