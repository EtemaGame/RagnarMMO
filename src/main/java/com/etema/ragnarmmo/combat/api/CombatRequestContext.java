package com.etema.ragnarmmo.combat.api;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Normalized server-side combat request context.
 */
public record CombatRequestContext(
        ServerPlayer actor,
        CombatActionType actionType,
        int sequenceId,
        int comboIndex,
        boolean offHand,
        String skillId,
        List<CombatTargetCandidate> candidates,
        Map<String, Object> metadata) {
    
    public CombatRequestContext(ServerPlayer actor, CombatActionType actionType, int sequenceId, int comboIndex, boolean offHand, String skillId, List<CombatTargetCandidate> candidates) {
        this(actor, actionType, sequenceId, comboIndex, offHand, skillId, candidates, java.util.Collections.emptyMap());
    }
}

