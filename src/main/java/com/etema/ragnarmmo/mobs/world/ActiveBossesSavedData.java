package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ActiveBossesSavedData extends SavedData {

    private static final String DATA_NAME = "ragnarmmo_active_bosses";

    private final Map<UUID, BossEntry> activeBosses = new LinkedHashMap<>();
    private final Map<String, RespawnEntry> respawnCooldowns = new LinkedHashMap<>();

    public static ActiveBossesSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                ActiveBossesSavedData::load,
                ActiveBossesSavedData::new,
                DATA_NAME);
    }

    public void registerBoss(ServerLevel level, LivingEntity entity, MobRank rank) {
        if (level == null || entity == null || rank == null || !BossRankRules.shouldPersistWorldState(rank)) {
            return;
        }

        BossEntry next = BossEntry.from(level, entity, rank);
        BossEntry previous = activeBosses.put(entity.getUUID(), next);
        boolean removedCooldown = false;
        if (next.hasSpawnKey()) {
            removedCooldown = respawnCooldowns.remove(next.spawnKey()) != null;
        }
        if (!next.equals(previous)) {
            setDirty();
        }
        if (removedCooldown) {
            setDirty();
        }

        RagnarDebugLog.bossWorld(
                "REGISTER boss={} tier={} source={} key={} respawnTicks={} dim={} pos=({}, {}, {}) replaced={} clearedCooldown={}",
                RagnarDebugLog.entityLabel(entity),
                rank,
                next.spawnSource(),
                next.spawnKey(),
                next.respawnDelayTicks(),
                next.dimensionId(),
                next.x(),
                next.y(),
                next.z(),
                previous != null,
                removedCooldown);
    }

    public void handleBossDeath(ServerLevel level, LivingEntity entity, MobRank rank) {
        if (level == null || entity == null || rank == null || !BossRankRules.shouldPersistWorldState(rank)) {
            return;
        }

        BossEntry removed = activeBosses.remove(entity.getUUID());
        BossEntry snapshot = removed != null ? removed : BossEntry.from(level, entity, rank);
        if (snapshot.hasRespawnRule()) {
            RespawnEntry next = RespawnEntry.fromDeath(snapshot, level.getGameTime());
            respawnCooldowns.put(snapshot.spawnKey(), next);
            RagnarDebugLog.bossWorld(
                    "DEFEAT boss={} tier={} key={} cooldownStart={} cooldownEnd={} delayTicks={} source={}",
                    RagnarDebugLog.entityLabel(entity),
                    rank,
                    next.spawnKey(),
                    next.lastDefeatedGameTime(),
                    next.nextAllowedGameTime(),
                    snapshot.respawnDelayTicks(),
                    snapshot.spawnSource());
        } else {
            RagnarDebugLog.bossWorld(
                    "DEFEAT boss={} tier={} key={} cooldown=none source={}",
                    RagnarDebugLog.entityLabel(entity),
                    rank,
                    snapshot.spawnKey(),
                    snapshot.spawnSource());
        }
        setDirty();
    }

    public void removeBoss(UUID entityUuid) {
        if (entityUuid != null && activeBosses.remove(entityUuid) != null) {
            setDirty();
        }
    }

    public Collection<BossEntry> getActiveBosses() {
        return Collections.unmodifiableCollection(activeBosses.values());
    }

    public Optional<BossEntry> getActiveBossEntry(UUID entityUuid) {
        if (entityUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeBosses.get(entityUuid));
    }

    public Collection<RespawnEntry> getRespawnCooldowns() {
        return Collections.unmodifiableCollection(respawnCooldowns.values());
    }

    public Optional<RespawnEntry> getRespawnEntry(String spawnKey) {
        if (spawnKey == null || spawnKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(respawnCooldowns.get(spawnKey.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public boolean removeRespawnCooldown(String spawnKey) {
        if (spawnKey == null || spawnKey.isBlank()) {
            return false;
        }

        String key = spawnKey.trim().toLowerCase(java.util.Locale.ROOT);
        boolean removed = respawnCooldowns.remove(key) != null;
        if (removed) {
            setDirty();
            RagnarDebugLog.bossWorld("CLEAR_COOLDOWN key={}", key);
        }
        return removed;
    }

    public boolean isSpawnKeyActive(String spawnKey) {
        if (spawnKey == null || spawnKey.isBlank()) {
            return false;
        }
        String key = spawnKey.trim().toLowerCase(java.util.Locale.ROOT);
        return activeBosses.values().stream().anyMatch(entry -> key.equals(entry.spawnKey()));
    }

    public boolean canSpawn(String spawnKey, long currentGameTime) {
        if (spawnKey == null || spawnKey.isBlank()) {
            return true;
        }
        String key = spawnKey.trim().toLowerCase(java.util.Locale.ROOT);
        if (isSpawnKeyActive(key)) {
            return false;
        }
        RespawnEntry entry = respawnCooldowns.get(key);
        return entry == null || currentGameTime >= entry.nextAllowedGameTime();
    }

    public int pruneAndRefresh(MinecraftServer server) {
        int changed = 0;
        Iterator<Map.Entry<UUID, BossEntry>> iterator = activeBosses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossEntry> entry = iterator.next();
            LivingEntity entity = findLivingEntity(server, entry.getKey()).orElse(null);
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                changed++;
                continue;
            }

            MobRank rank = BossRankResolver.resolveRank(entity).orElse(entry.getValue().rank());
            if (!BossRankRules.shouldPersistWorldState(rank)) {
                iterator.remove();
                changed++;
                continue;
            }

            ServerLevel level = (ServerLevel) entity.level();
            BossEntry refreshed = BossEntry.from(level, entity, rank);
            if (!refreshed.equals(entry.getValue())) {
                entry.setValue(refreshed);
                changed++;
            }
        }

        if (changed > 0) {
            setDirty();
            RagnarDebugLog.bossWorld(
                    "PRUNE refreshed={} active={} cooldowns={}",
                    changed,
                    activeBosses.size(),
                    respawnCooldowns.size());
        }
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag activeList = new ListTag();
        for (BossEntry entry : activeBosses.values()) {
            activeList.add(entry.save());
        }
        tag.put("activeBosses", activeList);

        ListTag respawnList = new ListTag();
        for (RespawnEntry entry : respawnCooldowns.values()) {
            respawnList.add(entry.save());
        }
        tag.put("respawnCooldowns", respawnList);
        return tag;
    }

    public static ActiveBossesSavedData load(CompoundTag tag) {
        ActiveBossesSavedData data = new ActiveBossesSavedData();

        ListTag activeList = tag.getList("activeBosses", Tag.TAG_COMPOUND);
        if (activeList.isEmpty() && tag.contains("bosses", Tag.TAG_LIST)) {
            activeList = tag.getList("bosses", Tag.TAG_COMPOUND);
        }

        for (int i = 0; i < activeList.size(); i++) {
            try {
                BossEntry entry = BossEntry.load(activeList.getCompound(i));
                data.activeBosses.put(entry.entityUuid(), entry);
            } catch (Exception ex) {
                RagnarMMO.LOGGER.warn("Failed to load active boss entry at index {}: {}", i, ex.getMessage());
            }
        }

        ListTag respawnList = tag.getList("respawnCooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < respawnList.size(); i++) {
            try {
                RespawnEntry entry = RespawnEntry.load(respawnList.getCompound(i));
                data.respawnCooldowns.put(entry.spawnKey(), entry);
            } catch (Exception ex) {
                RagnarMMO.LOGGER.warn("Failed to load boss respawn entry at index {}: {}", i, ex.getMessage());
            }
        }

        return data;
    }

    private static Optional<LivingEntity> findLivingEntity(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return Optional.empty();
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return Optional.of(living);
            }
        }
        return Optional.empty();
    }

    public record BossEntry(
            UUID entityUuid,
            String entityTypeId,
            String displayName,
            String dimensionId,
            int x,
            int y,
            int z,
            MobRank rank,
            BossSpawnSource spawnSource,
            String spawnKey,
            int respawnDelayTicks,
            long lastSeenGameTime) {

        public static BossEntry from(ServerLevel level, LivingEntity entity, MobRank rank) {
            BlockPos pos = entity.blockPosition();
            BossSpawnMetadata.SpawnInfo spawnInfo = BossSpawnMetadata.read(entity, rank);
            return new BossEntry(
                    entity.getUUID(),
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString(),
                    entity.getName().getString(),
                    level.dimension().location().toString(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    rank,
                    spawnInfo.source(),
                    spawnInfo.spawnKey(),
                    spawnInfo.respawnDelayTicks(),
                    level.getGameTime());
        }

        public boolean hasSpawnKey() {
            return spawnKey != null && !spawnKey.isBlank();
        }

        public boolean hasRespawnRule() {
            return hasSpawnKey() && respawnDelayTicks > 0 && spawnSource != null && spawnSource.isControlled();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("entityUuid", entityUuid);
            tag.putString("entityTypeId", entityTypeId);
            tag.putString("displayName", displayName);
            tag.putString("dimensionId", dimensionId);
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putString("rank", rank.name());
            tag.putString("spawnSource", spawnSource.name());
            tag.putString("spawnKey", spawnKey);
            tag.putInt("respawnDelayTicks", respawnDelayTicks);
            tag.putLong("lastSeenGameTime", lastSeenGameTime);
            return tag;
        }

        public static BossEntry load(CompoundTag tag) {
            BossSpawnSource spawnSource = BossSpawnSource.parse(tag.getString("spawnSource")).orElse(BossSpawnSource.NATURAL);
            return new BossEntry(
                    tag.getUUID("entityUuid"),
                    tag.getString("entityTypeId"),
                    tag.getString("displayName"),
                    tag.getString("dimensionId"),
                    tag.getInt("x"),
                    tag.getInt("y"),
                    tag.getInt("z"),
                    parseRank(tag),
                    spawnSource,
                    tag.getString("spawnKey"),
                    tag.getInt("respawnDelayTicks"),
                    tag.getLong("lastSeenGameTime"));
        }
    }

    public record RespawnEntry(
            String spawnKey,
            String entityTypeId,
            String displayName,
            String dimensionId,
            MobRank rank,
            BossSpawnSource spawnSource,
            int x,
            int y,
            int z,
            long lastDefeatedGameTime,
            long nextAllowedGameTime) {

        public static RespawnEntry fromDeath(BossEntry entry, long currentGameTime) {
            return new RespawnEntry(
                    entry.spawnKey(),
                    entry.entityTypeId(),
                    entry.displayName(),
                    entry.dimensionId(),
                    entry.rank(),
                    entry.spawnSource(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    currentGameTime,
                    currentGameTime + entry.respawnDelayTicks());
        }

        public boolean isReady(long currentGameTime) {
            return currentGameTime >= nextAllowedGameTime;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("spawnKey", spawnKey);
            tag.putString("entityTypeId", entityTypeId);
            tag.putString("displayName", displayName);
            tag.putString("dimensionId", dimensionId);
            tag.putString("rank", rank.name());
            tag.putString("spawnSource", spawnSource.name());
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putLong("lastDefeatedGameTime", lastDefeatedGameTime);
            tag.putLong("nextAllowedGameTime", nextAllowedGameTime);
            return tag;
        }

        public static RespawnEntry load(CompoundTag tag) {
            return new RespawnEntry(
                    tag.getString("spawnKey"),
                    tag.getString("entityTypeId"),
                    tag.getString("displayName"),
                    tag.getString("dimensionId"),
                    parseRank(tag),
                    BossSpawnSource.parse(tag.getString("spawnSource")).orElse(BossSpawnSource.DEBUG),
                    tag.getInt("x"),
                    tag.getInt("y"),
                    tag.getInt("z"),
                    tag.getLong("lastDefeatedGameTime"),
                    tag.getLong("nextAllowedGameTime"));
        }
    }

    private static MobRank parseRank(CompoundTag tag) {
        String value = tag.contains("rank") ? tag.getString("rank") : tag.getString("tier");
        try {
            return MobRank.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return MobRank.BOSS;
        }
    }
}
