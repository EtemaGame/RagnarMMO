package com.etema.ragnarmmo.combat.targeting;

import java.util.List;
import com.etema.ragnarmmo.combat.api.RagnarTargetCandidate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * RagnarTargetResolver - The authoritative boundary that resolves client-suggested
 * candidates into validated server entities.
 */
public interface RagnarTargetResolver {

    /**
     * Resolves and filters a list of candidates into a collection of validated entities.
     * 
     * @param player The attacker.
     * @param candidates Suggested targets from the request.
     * @return List of validated LivingEntity targets.
     */
    List<LivingEntity> resolveCandidates(ServerPlayer player, List<RagnarTargetCandidate> candidates);
}
