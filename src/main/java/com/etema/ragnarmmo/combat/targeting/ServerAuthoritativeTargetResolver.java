package com.etema.ragnarmmo.combat.targeting;

import java.util.ArrayList;
import java.util.List;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * ServerAuthoritativeTargetResolver - Implementation of target resolution
 * with strict server-side distance and validity checks.
 */
public final class ServerAuthoritativeTargetResolver implements RagnarTargetResolver {

    private static final double MAX_REACH_SQ = 64.0; // 8 blocks base

    @Override
    public List<LivingEntity> resolveCandidates(ServerPlayer player, List<RagnarTargetCandidate> candidates) {
        List<LivingEntity> resolved = new ArrayList<>();

        for (RagnarTargetCandidate candidate : candidates) {
            net.minecraft.world.entity.Entity entity = player.serverLevel().getEntity(candidate.entityId());
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            // check liveness
            if (!target.isAlive() || target.isInvulnerable()) {
                continue;
            }

            // distance check (sq for performance)
            double distSq = player.distanceToSqr(target);
            if (distSq > MAX_REACH_SQ) {
                continue;
            }

            // self-attack prevention
            if (target.getUUID().equals(player.getUUID())) {
                continue;
            }

            resolved.add(target);
        }

        return resolved;
    }
}
