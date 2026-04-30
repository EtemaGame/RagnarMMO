package com.etema.ragnarmmo.combat.state;

/**
 * Aggregates transient combat state used by the server-authoritative combat
 * layer.
 */
public class CombatActorState {
    private final CombatCooldownState cooldowns = new CombatCooldownState();
    private final CombatCastState castState = new CombatCastState();
    private int lastClientPacketSequenceId = -1;
    private int lastAcceptedSequenceId = -1;
    private long lastServerFallbackTick = Long.MIN_VALUE;
    private RecentBasicAttackIntent lastObservedPacketIntent = RecentBasicAttackIntent.none();

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

    public int getLastClientPacketSequenceId() {
        return lastClientPacketSequenceId;
    }

    public void setLastClientPacketSequenceId(int lastClientPacketSequenceId) {
        this.lastClientPacketSequenceId = lastClientPacketSequenceId;
    }

    public long getLastServerFallbackTick() {
        return lastServerFallbackTick;
    }

    public void setLastServerFallbackTick(long lastServerFallbackTick) {
        this.lastServerFallbackTick = lastServerFallbackTick;
    }

    public RecentBasicAttackIntent getLastObservedPacketIntent() {
        return lastObservedPacketIntent;
    }

    public void setLastObservedPacketIntent(RecentBasicAttackIntent lastObservedPacketIntent) {
        this.lastObservedPacketIntent = lastObservedPacketIntent == null ? RecentBasicAttackIntent.none() : lastObservedPacketIntent;
    }

    public record RecentBasicAttackIntent(long tick, int targetId, int sequenceId) {
        private static final RecentBasicAttackIntent NONE = new RecentBasicAttackIntent(Long.MIN_VALUE, -1, -1);

        public static RecentBasicAttackIntent none() {
            return NONE;
        }

        public boolean matchesRecent(long nowTick, int candidateTargetId) {
            return targetId == candidateTargetId && nowTick - tick >= 0 && nowTick - tick <= 1;
        }
    }
}
