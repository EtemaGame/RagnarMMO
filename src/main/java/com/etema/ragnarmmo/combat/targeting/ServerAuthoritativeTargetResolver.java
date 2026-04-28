package com.etema.ragnarmmo.combat.targeting;

import java.util.ArrayList;
import java.util.List;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import com.etema.ragnarmmo.combat.api.ResolvedTargetCandidate;
import com.etema.ragnarmmo.combat.api.TargetRejectReason;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * ServerAuthoritativeTargetResolver - Implementation of target resolution
 * with strict server-side distance and validity checks.
 */
public final class ServerAuthoritativeTargetResolver implements RagnarTargetResolver {

    private static final double MAX_REACH_SQ = 64.0; // 8 blocks base

    @Override
    public List<ResolvedTargetCandidate> resolveCandidates(ServerPlayer player, List<RagnarTargetCandidate> candidates) {
        List<ResolvedTargetCandidate> resolved = new ArrayList<>();

        for (RagnarTargetCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            Entity entity = player.serverLevel().getEntity(candidate.entityId());
            if (entity == null) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_NOT_FOUND));
                continue;
            }
            if (!(entity instanceof LivingEntity target)) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_NOT_LIVING));
                continue;
            }

            if (target.isRemoved()) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_REMOVED));
                continue;
            }

            if (!target.isAlive()) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_DEAD));
                continue;
            }

            if (target.isInvulnerable()) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_INVULNERABLE));
                continue;
            }

            // distance check (sq for performance)
            double distSq = player.distanceToSqr(target);
            if (distSq > MAX_REACH_SQ) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.TARGET_OUT_OF_RANGE));
                continue;
            }

            // self-attack prevention
            if (target.getUUID().equals(player.getUUID())) {
                resolved.add(ResolvedTargetCandidate.rejected(candidate.entityId(), TargetRejectReason.SELF_TARGET));
                continue;
            }

            resolved.add(ResolvedTargetCandidate.accepted(target));
        }

        return resolved;
    }
}
