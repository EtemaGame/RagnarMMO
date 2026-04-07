package com.etema.ragnarmmo.combat.validation;

import com.etema.ragnarmmo.combat.api.RagnarAttackRequest;
import com.etema.ragnarmmo.combat.state.CombatActorState;
import net.minecraft.server.level.ServerPlayer;

/**
 * AttackRequestValidator - Performs stateless and semi-stateful validation
 * of incoming combat requests to ensure architectural integrity.
 */
public final class AttackRequestValidator {

    private AttackRequestValidator() {
    }

    /**
     * Validates a basic attack request before it reaches the resolution engine.
     * 
     * @return null if valid, or a reason string if rejected.
     */
    public static String validate(ServerPlayer player, RagnarAttackRequest request, CombatActorState state) {
        if (player == null) return "PLAYER_NULL";
        if (!player.isAlive()) return "DEAD";
        if (player.isSpectator()) return "SPECTATOR";

        // basic payload limits
        if (request.candidates().size() > 16) {
            return "TOO_MANY_TARGETS";
        }

        // sequence validation (monotony)
        if (request.sequenceId() <= state.getLastAcceptedSequenceId()) {
            return "STALE_SEQUENCE";
        }

        // combo index safety
        if (request.comboIndex() < 0 || request.comboIndex() > 10) {
            return "INVALID_COMBO_INDEX";
        }

        return null;
    }
}
