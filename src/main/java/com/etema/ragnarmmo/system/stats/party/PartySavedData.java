package com.etema.ragnarmmo.system.stats.party;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent storage for all parties and invites.
 * Data survives server restarts.
 */
public class PartySavedData extends SavedData {

    private static final String DATA_NAME = "ragnarmmo_parties";

    // Parties indexed by UUID
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    // Player UUID -> Party UUID mapping for quick lookup
    private final Map<UUID, UUID> playerPartyMap = new ConcurrentHashMap<>();
    // Pending invites indexed by target player UUID
    private final Map<UUID, PartyInvite> pendingInvites = new ConcurrentHashMap<>();

    public PartySavedData() {
    }

    /**
     * Gets the PartySavedData instance for the server.
     * Uses the overworld's data storage.
     */
    public static PartySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
            PartySavedData::load,
            PartySavedData::new,
            DATA_NAME
        );
    }

    // === Party Management ===

    /**
     * Registers a new party and marks data dirty.
     */
    public void addParty(Party party) {
        if (party == null) return;
        parties.put(party.getPartyId(), party);
        for (UUID member : party.getMembers()) {
            playerPartyMap.put(member, party.getPartyId());
        }
        setDirty();
    }

    /**
     * Removes a party completely.
     */
    public void removeParty(UUID partyId) {
        Party party = parties.remove(partyId);
        if (party != null) {
            for (UUID member : party.getMembers()) {
                playerPartyMap.remove(member);
            }
            // Remove any pending invites for this party
            pendingInvites.entrySet().removeIf(e -> e.getValue().getPartyId().equals(partyId));
            setDirty();
        }
    }

    /**
     * Gets a party by its UUID.
     */
    public Party getParty(UUID partyId) {
        return partyId == null ? null : parties.get(partyId);
    }

    /**
     * Gets the party a player is in.
     */
    public Party getPartyByPlayer(UUID playerUUID) {
        if (playerUUID == null) return null;
        UUID partyId = playerPartyMap.get(playerUUID);
        return partyId != null ? parties.get(partyId) : null;
    }

    /**
     * Finds a party by name (case-insensitive).
     */
    public Party findPartyByName(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (Party party : parties.values()) {
            if (party.getName().toLowerCase(Locale.ROOT).equals(lowerName)) {
                return party;
            }
        }
        return null;
    }

    /**
     * Gets all parties.
     */
    public Collection<Party> getAllParties() {
        return Collections.unmodifiableCollection(parties.values());
    }

    /**
     * Checks if a player is in any party.
     */
    public boolean isPlayerInParty(UUID playerUUID) {
        return playerUUID != null && playerPartyMap.containsKey(playerUUID);
    }

    /**
     * Updates the player-party mapping when a player joins a party.
     */
    public void registerPlayerInParty(UUID playerUUID, UUID partyId) {
        if (playerUUID != null && partyId != null) {
            playerPartyMap.put(playerUUID, partyId);
            setDirty();
        }
    }

    /**
     * Removes a player from the mapping when they leave a party.
     */
    public void unregisterPlayerFromParty(UUID playerUUID) {
        if (playerUUID != null && playerPartyMap.remove(playerUUID) != null) {
            setDirty();
        }
    }

    /**
     * Marks data as dirty (needs saving).
     */
    public void markDirty() {
        setDirty();
    }

    // === Invite Management ===

    /**
     * Adds a pending invite.
     */
    public void addInvite(PartyInvite invite) {
        if (invite == null) return;
        pendingInvites.put(invite.getTargetUUID(), invite);
        setDirty();
    }

    /**
     * Gets and removes a pending invite for a player.
     */
    public PartyInvite consumeInvite(UUID targetUUID) {
        PartyInvite invite = pendingInvites.remove(targetUUID);
        if (invite != null) {
            setDirty();
        }
        return invite;
    }

    /**
     * Gets a pending invite without removing it.
     */
    public PartyInvite getInvite(UUID targetUUID) {
        return targetUUID == null ? null : pendingInvites.get(targetUUID);
    }

    /**
     * Checks if a player has a pending invite.
     */
    public boolean hasInvite(UUID targetUUID) {
        PartyInvite invite = pendingInvites.get(targetUUID);
        return invite != null && !invite.isExpired();
    }

    /**
     * Removes a pending invite.
     */
    public void removeInvite(UUID targetUUID) {
        if (pendingInvites.remove(targetUUID) != null) {
            setDirty();
        }
    }

    /**
     * Cleans up expired invites.
     * Should be called periodically.
     */
    public int cleanupExpiredInvites() {
        int removed = 0;
        Iterator<Map.Entry<UUID, PartyInvite>> it = pendingInvites.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
            RagnarMMO.LOGGER.debug("Cleaned up {} expired party invites", removed);
        }
        return removed;
    }

    /**
     * Cleans up invalid parties (no leader, no members).
     * Should be called on server start.
     */
    public int cleanupInvalidParties() {
        int removed = 0;
        Iterator<Map.Entry<UUID, Party>> it = parties.entrySet().iterator();
        while (it.hasNext()) {
            Party party = it.next().getValue();
            if (!party.isValid()) {
                // Remove player mappings
                for (UUID member : party.getMembers()) {
                    playerPartyMap.remove(member);
                }
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
            RagnarMMO.LOGGER.info("Cleaned up {} invalid parties", removed);
        }
        return removed;
    }

    /**
     * Rebuilds the player-party map from party data.
     * Useful after loading to ensure consistency.
     */
    private void rebuildPlayerPartyMap() {
        playerPartyMap.clear();
        for (Party party : parties.values()) {
            for (UUID member : party.getMembers()) {
                playerPartyMap.put(member, party.getPartyId());
            }
        }
    }

    // === NBT Serialization ===

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save parties
        ListTag partyList = new ListTag();
        for (Party party : parties.values()) {
            partyList.add(party.save());
        }
        tag.put("parties", partyList);

        // Save invites (only non-expired)
        ListTag inviteList = new ListTag();
        for (PartyInvite invite : pendingInvites.values()) {
            if (!invite.isExpired()) {
                inviteList.add(invite.save());
            }
        }
        tag.put("invites", inviteList);

        return tag;
    }

    public static PartySavedData load(CompoundTag tag) {
        PartySavedData data = new PartySavedData();

        // Load parties
        ListTag partyList = tag.getList("parties", Tag.TAG_COMPOUND);
        for (int i = 0; i < partyList.size(); i++) {
            try {
                Party party = Party.load(partyList.getCompound(i));
                if (party.isValid()) {
                    data.parties.put(party.getPartyId(), party);
                }
            } catch (Exception e) {
                RagnarMMO.LOGGER.warn("Failed to load party at index {}: {}", i, e.getMessage());
            }
        }

        // Load invites
        ListTag inviteList = tag.getList("invites", Tag.TAG_COMPOUND);
        for (int i = 0; i < inviteList.size(); i++) {
            try {
                PartyInvite invite = PartyInvite.load(inviteList.getCompound(i));
                if (!invite.isExpired()) {
                    data.pendingInvites.put(invite.getTargetUUID(), invite);
                }
            } catch (Exception e) {
                RagnarMMO.LOGGER.warn("Failed to load invite at index {}: {}", i, e.getMessage());
            }
        }

        // Rebuild player-party map
        data.rebuildPlayerPartyMap();

        RagnarMMO.LOGGER.info("Loaded {} parties and {} pending invites",
            data.parties.size(), data.pendingInvites.size());

        return data;
    }

    // === Statistics ===

    public int getPartyCount() {
        return parties.size();
    }

    public int getInviteCount() {
        return pendingInvites.size();
    }

    public int getTotalMembersInParties() {
        return playerPartyMap.size();
    }
}
