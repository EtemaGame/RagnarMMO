package com.etema.ragnarmmo.combat.targeting;

import java.util.List;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import com.etema.ragnarmmo.combat.api.ResolvedTargetCandidate;
import net.minecraft.server.level.ServerPlayer;

/**
 * RagnarTargetResolver - The authoritative boundary that resolves client-suggested
 * candidates into validated server entities.
 */
public interface RagnarTargetResolver {

    /**
     * Resolves candidates into per-target results, preserving rejection reasons.
     * 
     * @param player The attacker.
     * @param candidates Suggested targets from the request.
     * @return List of per-candidate resolution results.
     */
    List<ResolvedTargetCandidate> resolveCandidates(ServerPlayer player, List<RagnarTargetCandidate> candidates);
}
