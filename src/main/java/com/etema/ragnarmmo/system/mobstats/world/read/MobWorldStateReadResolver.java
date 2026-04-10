package com.etema.ragnarmmo.system.mobstats.world.read;

import com.etema.ragnarmmo.system.mobstats.world.ActiveBossesSavedData;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnMetadata;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Explicit read boundary for minimal boss/world-state inspection during coexistence.
 *
 * <p>This resolver is intentionally separate from the shared semantic mob read surface. It exposes
 * only minimal lifecycle, cooldown, and defeat-history reads and does not carry encounter
 * semantics such as rank.</p>
 */
public final class MobWorldStateReadResolver {

    private MobWorldStateReadResolver() {
    }

    public static Optional<MobWorldStateEntityReadView> resolveEntity(
            MinecraftServer server,
            UUID entityUuid) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(entityUuid, "entityUuid");

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityUuid);
            if (entity instanceof LivingEntity living) {
                return resolveEntity(living);
            }
        }

        return Optional.empty();
    }

    public static Optional<MobWorldStateEntityReadView> resolveEntity(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        ActiveBossesSavedData data = ActiveBossesSavedData.get(serverLevel.getServer());
        Optional<ActiveBossesSavedData.BossEntry> activeEntry = data.getActiveBossEntry(entity.getUUID());
        Optional<BossSpawnMetadata.SpawnInfo> explicitSpawnInfo = BossSpawnMetadata.readExplicit(entity);

        String encounterKey = activeEntry
                .map(ActiveBossesSavedData.BossEntry::spawnKey)
                .filter(MobWorldStateReadResolver::hasText)
                .or(() -> explicitSpawnInfo
                        .map(BossSpawnMetadata.SpawnInfo::spawnKey)
                        .filter(MobWorldStateReadResolver::hasText))
                .orElse(null);

        BossSpawnSource spawnSource = activeEntry
                .map(ActiveBossesSavedData.BossEntry::spawnSource)
                .or(() -> explicitSpawnInfo.map(BossSpawnMetadata.SpawnInfo::source))
                .orElse(null);

        Integer respawnDelayTicks = activeEntry
                .map(ActiveBossesSavedData.BossEntry::respawnDelayTicks)
                .or(() -> explicitSpawnInfo.map(BossSpawnMetadata.SpawnInfo::respawnDelayTicks))
                .orElse(null);

        Optional<ActiveBossesSavedData.RespawnEntry> respawnEntry = hasText(encounterKey)
                ? data.getRespawnEntry(encounterKey)
                : Optional.empty();

        return Optional.of(new MobWorldStateEntityReadView(
                activeEntry.isPresent(),
                encounterKey,
                spawnSource,
                respawnDelayTicks,
                respawnEntry.isPresent(),
                respawnEntry.map(entry -> entry.isReady(serverLevel.getGameTime())).orElse(false),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::nextAllowedGameTime).orElse(null),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::lastDefeatedGameTime).orElse(null)));
    }

    public static List<MobWorldStateActiveEntryReadView> listActiveEntries(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        ActiveBossesSavedData data = ActiveBossesSavedData.get(server);
        long currentGameTime = server.overworld().getGameTime();
        List<MobWorldStateActiveEntryReadView> views = new ArrayList<>();
        for (ActiveBossesSavedData.BossEntry entry : data.getActiveBosses()) {
            String encounterKey = normalizeOptionalEncounterKey(entry.spawnKey());
            Optional<ActiveBossesSavedData.RespawnEntry> respawnEntry = encounterKey != null
                    ? data.getRespawnEntry(encounterKey)
                    : Optional.empty();

            views.add(new MobWorldStateActiveEntryReadView(
                    entry.entityUuid(),
                    entry.displayName(),
                    entry.dimensionId(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    true,
                    encounterKey,
                    entry.spawnSource(),
                    entry.respawnDelayTicks(),
                    respawnEntry.isPresent(),
                    respawnEntry.map(respawn -> respawn.isReady(currentGameTime)).orElse(false),
                    respawnEntry.map(ActiveBossesSavedData.RespawnEntry::nextAllowedGameTime).orElse(null),
                    respawnEntry.map(ActiveBossesSavedData.RespawnEntry::lastDefeatedGameTime).orElse(null)));
        }

        views.sort(Comparator.comparing(MobWorldStateActiveEntryReadView::displayName));
        return List.copyOf(views);
    }

    public static Optional<MobWorldStateEncounterReadView> resolveEncounter(
            MinecraftServer server,
            String encounterKey) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(encounterKey, "encounterKey");

        String normalizedKey = normalizeEncounterKey(encounterKey);
        if (normalizedKey.isBlank()) {
            return Optional.empty();
        }

        ActiveBossesSavedData data = ActiveBossesSavedData.get(server);
        Optional<ActiveBossesSavedData.RespawnEntry> respawnEntry = data.getRespawnEntry(normalizedKey);
        long currentGameTime = server.overworld().getGameTime();

        return Optional.of(new MobWorldStateEncounterReadView(
                normalizedKey,
                data.isSpawnKeyActive(normalizedKey),
                respawnEntry.isPresent(),
                respawnEntry.map(entry -> entry.isReady(currentGameTime)).orElse(false),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::nextAllowedGameTime).orElse(null),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::lastDefeatedGameTime).orElse(null)));
    }

    public static Optional<MobWorldStateCooldownEntryReadView> resolveCooldownEntry(
            MinecraftServer server,
            String encounterKey) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(encounterKey, "encounterKey");

        String normalizedKey = normalizeEncounterKey(encounterKey);
        if (normalizedKey.isBlank()) {
            return Optional.empty();
        }

        ActiveBossesSavedData data = ActiveBossesSavedData.get(server);
        Optional<ActiveBossesSavedData.RespawnEntry> respawnEntry = data.getRespawnEntry(normalizedKey);
        Optional<ActiveBossesSavedData.BossEntry> activeEntry = data.getActiveBosses().stream()
                .filter(entry -> normalizedKey.equals(normalizeOptionalEncounterKey(entry.spawnKey())))
                .findFirst();

        if (respawnEntry.isEmpty() && activeEntry.isEmpty()) {
            return Optional.empty();
        }

        long currentGameTime = server.overworld().getGameTime();
        String displayName = respawnEntry.map(ActiveBossesSavedData.RespawnEntry::displayName)
                .or(() -> activeEntry.map(ActiveBossesSavedData.BossEntry::displayName))
                .orElse("<unknown>");
        String dimensionId = respawnEntry.map(ActiveBossesSavedData.RespawnEntry::dimensionId)
                .or(() -> activeEntry.map(ActiveBossesSavedData.BossEntry::dimensionId))
                .orElse("<unknown>");

        return Optional.of(new MobWorldStateCooldownEntryReadView(
                normalizedKey,
                displayName,
                dimensionId,
                activeEntry.isPresent(),
                respawnEntry.isPresent(),
                respawnEntry.map(entry -> entry.isReady(currentGameTime)).orElse(false),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::nextAllowedGameTime).orElse(null),
                respawnEntry.map(ActiveBossesSavedData.RespawnEntry::lastDefeatedGameTime).orElse(null)));
    }

    public static List<MobWorldStateCooldownEntryReadView> listCooldownEntries(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        ActiveBossesSavedData data = ActiveBossesSavedData.get(server);
        long currentGameTime = server.overworld().getGameTime();
        List<MobWorldStateCooldownEntryReadView> views = new ArrayList<>();
        for (ActiveBossesSavedData.RespawnEntry entry : data.getRespawnCooldowns()) {
            views.add(new MobWorldStateCooldownEntryReadView(
                    entry.spawnKey(),
                    entry.displayName(),
                    entry.dimensionId(),
                    data.isSpawnKeyActive(entry.spawnKey()),
                    true,
                    entry.isReady(currentGameTime),
                    entry.nextAllowedGameTime(),
                    entry.lastDefeatedGameTime()));
        }

        views.sort(Comparator.comparing(MobWorldStateCooldownEntryReadView::encounterKey));
        return List.copyOf(views);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeOptionalEncounterKey(String encounterKey) {
        if (!hasText(encounterKey)) {
            return null;
        }
        return normalizeEncounterKey(encounterKey);
    }

    private static String normalizeEncounterKey(String encounterKey) {
        return encounterKey.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
