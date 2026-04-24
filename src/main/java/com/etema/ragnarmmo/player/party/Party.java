package com.etema.ragnarmmo.player.party;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Represents a persistent party of players.
 * Parties can share XP, have a leader, and support up to 6 members.
 * All data is persisted via PartySavedData.
 */
public class Party {
    public static final int MAX_PARTY_SIZE = 6;

    private final UUID partyId;
    private String name;
    private UUID leaderUUID;
    private final Set<UUID> members;
    private final PartySettings settings;

    // Timestamps
    private long createdAt;
    private long lastActivity;

    /**
     * Creates a new party with a random UUID.
     */
    public Party(String name, UUID leaderUUID) {
        this.partyId = UUID.randomUUID();
        this.name = name;
        this.leaderUUID = leaderUUID;
        this.members = new LinkedHashSet<>(); // Preserve join order
        this.members.add(leaderUUID);
        this.settings = new PartySettings();
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = this.createdAt;
    }

    /**
     * Creates a party from NBT data (for persistence).
     */
    private Party(UUID partyId, String name, UUID leaderUUID, Set<UUID> members,
                  PartySettings settings, long createdAt, long lastActivity) {
        this.partyId = partyId;
        this.name = name;
        this.leaderUUID = leaderUUID;
        this.members = new LinkedHashSet<>(members);
        this.settings = settings;
        this.createdAt = createdAt;
        this.lastActivity = lastActivity;
    }

    // === Basic Info ===

    public UUID getPartyId() {
        return partyId;
    }

    public String getId() {
        return partyId.toString().substring(0, 8);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateActivity();
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public void setLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leaderUUID = newLeader;
            updateActivity();
        }
    }

    public boolean isLeader(UUID playerUUID) {
        return playerUUID != null && playerUUID.equals(leaderUUID);
    }

    // === Members ===

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= MAX_PARTY_SIZE;
    }

    public boolean isMember(UUID playerUUID) {
        return playerUUID != null && members.contains(playerUUID);
    }

    public boolean addMember(UUID playerUUID) {
        if (isFull() || playerUUID == null) {
            return false;
        }
        boolean added = members.add(playerUUID);
        if (added) {
            updateActivity();
        }
        return added;
    }

    public boolean removeMember(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        boolean removed = members.remove(playerUUID);
        if (removed) {
            updateActivity();
            // If leader left, promote next member
            if (playerUUID.equals(leaderUUID) && !members.isEmpty()) {
                leaderUUID = members.iterator().next();
            }
        }
        return removed;
    }

    // === Settings ===

    public PartySettings getSettings() {
        return settings;
    }

    // === Timestamps ===

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    // === Online Members ===

    /**
     * Gets all online members of this party.
     */
    public List<ServerPlayer> getOnlineMembers(MinecraftServer server) {
        List<ServerPlayer> online = new ArrayList<>();
        if (server == null) return online;

        for (UUID memberId : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    /**
     * Gets online members within range of a source player.
     */
    public List<ServerPlayer> getNearbyMembers(ServerPlayer source, boolean sameDimension) {
        List<ServerPlayer> nearby = new ArrayList<>();
        if (source == null || source.getServer() == null) return nearby;

        double range = settings.getShareRange();
        double rangeSq = range * range;

        for (UUID memberId : members) {
            if (memberId.equals(source.getUUID())) continue;

            ServerPlayer member = source.getServer().getPlayerList().getPlayer(memberId);
            if (member == null) continue;

            // Check dimension
            if (sameDimension && !member.level().dimension().equals(source.level().dimension())) {
                continue;
            }

            // Check range
            double distSq = source.distanceToSqr(member);
            if (distSq <= rangeSq) {
                nearby.add(member);
            }
        }
        return nearby;
    }

    /**
     * Gets eligible members for XP sharing (online, in range, same dimension).
     */
    public List<ServerPlayer> getEligibleMembersForXp(ServerPlayer source) {
        List<ServerPlayer> eligible = new ArrayList<>();
        eligible.add(source); // Always include the source

        if (!settings.isXpShareEnabled()) {
            return eligible;
        }

        eligible.addAll(getNearbyMembers(source, true));
        return eligible;
    }

    // === NBT Serialization ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("partyId", partyId);
        tag.putString("name", name);
        tag.putUUID("leader", leaderUUID);
        tag.putLong("createdAt", createdAt);
        tag.putLong("lastActivity", lastActivity);

        // Members
        ListTag memberList = new ListTag();
        for (UUID member : members) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.putUUID("uuid", member);
            memberList.add(memberTag);
        }
        tag.put("members", memberList);

        // Settings
        tag.put("settings", settings.save());

        return tag;
    }

    public static Party load(CompoundTag tag) {
        UUID partyId = tag.getUUID("partyId");
        String name = tag.getString("name");
        UUID leader = tag.getUUID("leader");
        long createdAt = tag.getLong("createdAt");
        long lastActivity = tag.getLong("lastActivity");

        // Members
        Set<UUID> members = new LinkedHashSet<>();
        ListTag memberList = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            CompoundTag memberTag = memberList.getCompound(i);
            members.add(memberTag.getUUID("uuid"));
        }

        // Settings
        PartySettings settings = PartySettings.load(tag.getCompound("settings"));

        return new Party(partyId, name, leader, members, settings, createdAt, lastActivity);
    }

    // === Validation ===

    /**
     * Checks if this party is valid (has leader and at least one member).
     */
    public boolean isValid() {
        return leaderUUID != null && !members.isEmpty() && members.contains(leaderUUID);
    }

    // === Utils ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return partyId.equals(party.partyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partyId);
    }

    @Override
    public String toString() {
        return String.format("Party[%s] '%s' (%d members)",
                getId(), name, members.size());
    }
}
