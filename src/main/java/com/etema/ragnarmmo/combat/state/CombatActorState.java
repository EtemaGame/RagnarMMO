package com.etema.ragnarmmo.combat.state;

/**
 * Aggregates transient combat state used by the server-authoritative combat
 * layer.
 */
public class CombatActorState {
    private final CombatCooldownState cooldowns = new CombatCooldownState();
    private final CombatCastState castState = new CombatCastState();
    private int lastAcceptedSequenceId = -1;

    public CombatCooldownState getCooldowns() {
        return cooldowns;
    }

    public CombatCastState getCastState() {
        return castState;
    }

    public int getLastAcceptedSequenceId() {
        return lastAcceptedSequenceId;
    }

    public void setLastAcceptedSequenceId(int lastAcceptedSequenceId) {
        this.lastAcceptedSequenceId = lastAcceptedSequenceId;
    }
}
