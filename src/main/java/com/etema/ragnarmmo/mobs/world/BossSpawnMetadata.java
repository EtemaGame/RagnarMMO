package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class BossSpawnMetadata {

    private static final String ROOT_KEY = "RagnarBossSpawn";
    private static final String SOURCE_KEY = "Source";
    private static final String SPAWN_KEY = "SpawnKey";
    private static final String RESPAWN_DELAY_KEY = "RespawnDelayTicks";

    private BossSpawnMetadata() {
    }

    public static SpawnInfo read(LivingEntity entity, MobRank fallbackRank) {
        if (entity == null) {
            return SpawnInfo.natural("", 0);
        }

        CompoundTag root = getRoot(entity);
        if (root == null) {
            return SpawnInfo.natural("", 0);
        }
        BossSpawnSource source = BossSpawnSource.parse(root.getString(SOURCE_KEY)).orElse(BossSpawnSource.NATURAL);
        String spawnKey = root.getString(SPAWN_KEY);
        int respawnDelayTicks = Math.max(0, root.getInt(RESPAWN_DELAY_KEY));

        if (fallbackRank != null && BossRankRules.shouldPersistWorldState(fallbackRank) && source == BossSpawnSource.NATURAL
                && respawnDelayTicks <= 0) {
            return SpawnInfo.natural(spawnKey, 0);
        }

        return new SpawnInfo(source, sanitizeSpawnKey(spawnKey), respawnDelayTicks);
    }

    public static Optional<SpawnInfo> readExplicit(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        CompoundTag root = getRoot(entity);
        if (root == null) {
            return Optional.empty();
        }

        BossSpawnSource source = BossSpawnSource.parse(root.getString(SOURCE_KEY)).orElse(BossSpawnSource.NATURAL);
        String spawnKey = sanitizeSpawnKey(root.getString(SPAWN_KEY));
        int respawnDelayTicks = Math.max(0, root.getInt(RESPAWN_DELAY_KEY));
        return Optional.of(new SpawnInfo(source, spawnKey, respawnDelayTicks));
    }

    public static void markNatural(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        CompoundTag root = getOrCreateRoot(entity);
        root.putString(SOURCE_KEY, BossSpawnSource.NATURAL.name());
        root.remove(SPAWN_KEY);
        root.putInt(RESPAWN_DELAY_KEY, 0);
    }

    public static void markControlled(LivingEntity entity, BossSpawnSource source, String spawnKey, int respawnDelayTicks) {
        if (entity == null) {
            return;
        }

        CompoundTag root = getOrCreateRoot(entity);
        BossSpawnSource resolvedSource = source == null ? BossSpawnSource.DEBUG : source;
        root.putString(SOURCE_KEY, resolvedSource.name());
        String cleanedKey = sanitizeSpawnKey(spawnKey);
        if (cleanedKey.isBlank()) {
            root.remove(SPAWN_KEY);
        } else {
            root.putString(SPAWN_KEY, cleanedKey);
        }
        root.putInt(RESPAWN_DELAY_KEY, Math.max(0, respawnDelayTicks));
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        entity.getPersistentData().remove(ROOT_KEY);
    }

    public static Optional<String> getSpawnKey(LivingEntity entity) {
        String spawnKey = read(entity, null).spawnKey();
        return spawnKey.isBlank() ? Optional.empty() : Optional.of(spawnKey);
    }

    private static CompoundTag getOrCreateRoot(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ROOT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            data.put(ROOT_KEY, new CompoundTag());
        }
        return data.getCompound(ROOT_KEY);
    }

    private static CompoundTag getRoot(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ROOT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        return data.getCompound(ROOT_KEY);
    }

    private static String sanitizeSpawnKey(String spawnKey) {
        return spawnKey == null ? "" : spawnKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record SpawnInfo(BossSpawnSource source, String spawnKey, int respawnDelayTicks) {
        public static SpawnInfo natural(String spawnKey, int respawnDelayTicks) {
            return new SpawnInfo(BossSpawnSource.NATURAL, spawnKey == null ? "" : spawnKey, Math.max(0, respawnDelayTicks));
        }

        public boolean isControlled() {
            return source != null && source.isControlled() && !spawnKey.isBlank();
        }
    }
}
