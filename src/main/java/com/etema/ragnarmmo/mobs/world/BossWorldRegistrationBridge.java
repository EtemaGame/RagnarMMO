package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Objects;

/**
 * Registers canonical mob ranks in world-state persistence.
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
            ActiveBossesSavedData.get(serverLevel.getServer()).registerBoss(serverLevel, entity, rank);
            RagnarDebugLog.bossWorld("Bridge: Registered rank {} mob {}", rank, RagnarDebugLog.entityLabel(entity));
        }
    }
}
