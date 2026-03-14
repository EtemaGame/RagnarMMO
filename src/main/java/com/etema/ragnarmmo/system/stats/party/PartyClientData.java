package com.etema.ragnarmmo.system.stats.party;

import com.etema.ragnarmmo.system.stats.party.net.PartyMemberData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

/**
 * Client-side storage for party data received from the server.
 * This class is only used on the client and holds the data needed for HUD rendering.
 */
@OnlyIn(Dist.CLIENT)
public class PartyClientData {

    private static UUID partyId = null;
    private static String partyName = "";
    private static final Map<UUID, PartyMemberData> members = new LinkedHashMap<>();
    private static long lastUpdateTime = 0;

    // Track if data changed for HUD optimization
    private static boolean dirty = false;

    private PartyClientData() {}

    // === Data Access ===

    public static boolean hasParty() {
        return partyId != null;
    }

    public static UUID getPartyId() {
        return partyId;
    }

    public static String getPartyName() {
        return partyName;
    }

    public static Collection<PartyMemberData> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    public static List<PartyMemberData> getMembersList() {
        return new ArrayList<>(members.values());
    }

    public static int getMemberCount() {
        return members.size();
    }

    public static PartyMemberData getMember(UUID uuid) {
        return members.get(uuid);
    }

    public static PartyMemberData getLeader() {
        for (PartyMemberData member : members.values()) {
            if (member.isLeader()) {
                return member;
            }
        }
        return null;
    }

    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }

    // === Data Update (called from network handlers) ===

    /**
     * Sets the full party data from a snapshot packet.
     */
    public static void setParty(UUID id, String name, List<PartyMemberData> memberList) {
        partyId = id;
        partyName = name;
        members.clear();

        if (memberList != null) {
            for (PartyMemberData member : memberList) {
                members.put(member.uuid(), member);
            }
        }

        lastUpdateTime = System.currentTimeMillis();
        dirty = true;
    }

    /**
     * Clears party data (player left party or was kicked).
     */
    public static void clearParty() {
        partyId = null;
        partyName = "";
        members.clear();
        lastUpdateTime = System.currentTimeMillis();
        dirty = true;
    }

    /**
     * Updates a single member's data.
     */
    public static void updateMember(PartyMemberData memberData) {
        if (memberData == null || partyId == null) return;

        members.put(memberData.uuid(), memberData);
        lastUpdateTime = System.currentTimeMillis();
        dirty = true;
    }

    /**
     * Removes a member from the client data.
     */
    public static void removeMember(UUID uuid) {
        if (uuid == null) return;
        if (members.remove(uuid) != null) {
            lastUpdateTime = System.currentTimeMillis();
            dirty = true;
        }
    }

    // === HUD Optimization ===

    /**
     * Returns true if data changed since last check, and resets the flag.
     */
    public static boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    /**
     * Returns true if data changed (without consuming).
     */
    public static boolean isDirty() {
        return dirty;
    }

    // === Sorting ===

    /**
     * Gets members sorted with leader first.
     */
    public static List<PartyMemberData> getMembersSortedLeaderFirst() {
        List<PartyMemberData> sorted = new ArrayList<>(members.values());
        sorted.sort((a, b) -> {
            if (a.isLeader() && !b.isLeader()) return -1;
            if (!a.isLeader() && b.isLeader()) return 1;
            return 0;
        });
        return sorted;
    }

    /**
     * Gets members sorted by join order (default LinkedHashMap order).
     */
    public static List<PartyMemberData> getMembersSortedByJoinOrder() {
        return new ArrayList<>(members.values());
    }

    /**
     * Gets members sorted by online status (online first).
     */
    public static List<PartyMemberData> getMembersSortedByOnlineStatus() {
        List<PartyMemberData> sorted = new ArrayList<>(members.values());
        sorted.sort((a, b) -> {
            if (a.isOnline() && !b.isOnline()) return -1;
            if (!a.isOnline() && b.isOnline()) return 1;
            // Leader first among same online status
            if (a.isLeader() && !b.isLeader()) return -1;
            if (!a.isLeader() && b.isLeader()) return 1;
            return 0;
        });
        return sorted;
    }
}
