package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Locale;
import java.util.UUID;

public final class BossSpawnService {

    private BossSpawnService() {
    }

    public static SpawnResult spawnControlledBoss(
            ServerLevel level,
            BlockPos pos,
            EntityType<?> entityType,
            MobRank rank,
            BossSpawnSource spawnSource,
            String spawnKey,
            int respawnSeconds) {
        if (level == null || pos == null || entityType == null) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=missing_context pos={} tier={} source={} key={}",
                    RagnarDebugLog.blockPos(pos), rank, spawnSource, spawnKey);
            return SpawnResult.failure("Missing level, position or entity type.");
        }
        if (rank == null || !BossRankRules.isControlledSpawnRank(rank)) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=invalid_rank entityType={} rank={} source={} key={}",
                    entityType, rank, spawnSource, spawnKey);
            return SpawnResult.failure("Controlled spawns require MINI_BOSS or BOSS rank.");
        }
        if (spawnSource == null || !spawnSource.isControlled()) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=invalid_source entityType={} rank={} source={} key={}",
                    entityType, rank, spawnSource, spawnKey);
            return SpawnResult.failure("Controlled spawns require a non-natural source.");
        }

        String normalizedKey = normalizeKey(spawnKey);
        if (normalizedKey.isBlank()) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=empty_key entityType={} rank={} source={} pos={}",
                    entityType, rank, spawnSource, RagnarDebugLog.blockPos(pos));
            return SpawnResult.failure("Spawn key cannot be empty.");
        }

        ActiveBossesSavedData data = ActiveBossesSavedData.get(level.getServer());
        if (!data.canSpawn(normalizedKey, level.getGameTime())) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=active_or_cooldown entityType={} rank={} source={} key={} gameTime={}",
                    entityType, rank, spawnSource, normalizedKey, level.getGameTime());
            return SpawnResult.failure("Spawn key is active or still on cooldown.");
        }

        Entity entity = entityType.create(level);
        if (!(entity instanceof LivingEntity living)) {
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=non_living entityType={} rank={} source={} key={}",
                    entityType, rank, spawnSource, normalizedKey);
            return SpawnResult.failure("Selected entity type is not a living entity.");
        }

        float yaw = level.random.nextFloat() * 360.0F;
        living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);

        if (living instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
        }

        MobSpawnOverrides.setForcedRank(living, rank);
        BossSpawnMetadata.markControlled(living, spawnSource, normalizedKey, Math.max(0, respawnSeconds) * 20);

        boolean added = level.addFreshEntity(living);
        if (!added) {
            MobSpawnOverrides.clear(living);
            BossSpawnMetadata.clear(living);
            RagnarDebugLog.bossWorld("SPAWN_CONTROLLED result=FAIL reason=add_rejected entityType={} rank={} source={} key={} pos={}",
                    entityType, rank, spawnSource, normalizedKey, RagnarDebugLog.blockPos(pos));
            return SpawnResult.failure("Minecraft rejected the boss spawn.");
        }

        RagnarDebugLog.bossWorld(
                "SPAWN_CONTROLLED result=OK entity={} rank={} source={} key={} respawnSeconds={} pos={}",
                RagnarDebugLog.entityLabel(living),
                rank,
                spawnSource,
                normalizedKey,
                Math.max(0, respawnSeconds),
                RagnarDebugLog.blockPos(pos));
        return SpawnResult.success(living.getUUID(), normalizedKey);
    }

    private static String normalizeKey(String spawnKey) {
        return spawnKey == null ? "" : spawnKey.trim().toLowerCase(Locale.ROOT);
    }

    public record SpawnResult(boolean success, String message, UUID entityUuid, String spawnKey) {
        public static SpawnResult success(UUID entityUuid, String spawnKey) {
            return new SpawnResult(true, "ok", entityUuid, spawnKey);
        }

        public static SpawnResult failure(String message) {
            return new SpawnResult(false, message, null, "");
        }
    }
}
