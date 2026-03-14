package com.etema.ragnarmmo.system.stats.party;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Represents an invitation to join a party.
 * Invites expire after a configurable time.
 */
public class PartyInvite {

    private static final long DEFAULT_EXPIRE_TIME_MS = 60_000; // 60 seconds

    private final UUID targetUUID;
    private final UUID inviterUUID;
    private final UUID partyId;
    private final long createdAt;
    private final long expiresAt;

    public PartyInvite(UUID targetUUID, UUID inviterUUID, UUID partyId) {
        this(targetUUID, inviterUUID, partyId, System.currentTimeMillis(),
             System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_MS);
    }

    public PartyInvite(UUID targetUUID, UUID inviterUUID, UUID partyId, long expiresInMs) {
        this(targetUUID, inviterUUID, partyId, System.currentTimeMillis(),
             System.currentTimeMillis() + expiresInMs);
    }

    private PartyInvite(UUID targetUUID, UUID inviterUUID, UUID partyId,
                        long createdAt, long expiresAt) {
        this.targetUUID = targetUUID;
        this.inviterUUID = inviterUUID;
        this.partyId = partyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // === Getters ===

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public UUID getInviterUUID() {
        return inviterUUID;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTimeMs() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public int getRemainingTimeSeconds() {
        return (int) (getRemainingTimeMs() / 1000);
    }

    // === NBT Serialization ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("target", targetUUID);
        tag.putUUID("inviter", inviterUUID);
        tag.putUUID("partyId", partyId);
        tag.putLong("createdAt", createdAt);
        tag.putLong("expiresAt", expiresAt);
        return tag;
    }

    public static PartyInvite load(CompoundTag tag) {
        UUID target = tag.getUUID("target");
        UUID inviter = tag.getUUID("inviter");
        UUID partyId = tag.getUUID("partyId");
        long createdAt = tag.getLong("createdAt");
        long expiresAt = tag.getLong("expiresAt");

        return new PartyInvite(target, inviter, partyId, createdAt, expiresAt);
    }

    @Override
    public String toString() {
        return String.format("PartyInvite[target=%s, party=%s, expires=%ds]",
                targetUUID.toString().substring(0, 8),
                partyId.toString().substring(0, 8),
                getRemainingTimeSeconds());
    }
}
