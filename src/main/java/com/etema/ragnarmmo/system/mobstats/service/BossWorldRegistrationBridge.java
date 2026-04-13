package com.etema.ragnarmmo.system.mobstats.service;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.system.mobstats.world.ActiveBossesSavedData;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnMetadata;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Objects;

/**
 * Bridges the gap between new mob ranks and world-level persistence logic.
 */
public final class BossWorldRegistrationBridge {

    private BossWorldRegistrationBridge() {
    }

    /**
     * Handles persistence and world-state registration for non-normal entities.
     */
    public static void handleRegistration(LivingEntity entity, MobRank rank) {
        Objects.requireNonNull(entity, "entity");
        
        if (rank == null || rank == MobRank.NORMAL) {
            return;
        }

        // Persistence
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        // Natural marking
        if (BossSpawnMetadata.getSpawnKey(entity).isEmpty()) {
            BossSpawnMetadata.markNatural(entity);
        }

        // World Saved Data registration
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getServer() != null) {
            
            MobTier legacyTier = mapRankToTier(rank);
            ActiveBossesSavedData.get(serverLevel.getServer()).registerBoss(serverLevel, entity, legacyTier);
            
            RagnarDebugLog.bossWorld("Bridge: Registered rank {} mob {} as legacy tier {}", 
                    rank, RagnarDebugLog.entityLabel(entity), legacyTier);
        }
    }

    private static MobTier mapRankToTier(MobRank rank) {
        return switch (rank) {
            case ELITE -> MobTier.ELITE;
            case BOSS -> MobTier.BOSS;
            default -> MobTier.NORMAL;
        };
    }
}
