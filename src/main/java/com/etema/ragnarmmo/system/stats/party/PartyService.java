package com.etema.ragnarmmo.system.stats.party;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.stats.party.net.PartyMemberData;
import com.etema.ragnarmmo.system.stats.party.net.PartyMemberUpdateS2CPacket;
import com.etema.ragnarmmo.system.stats.party.net.PartySnapshotS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Centralized service for all party operations.
 * All business logic goes here - commands and events should delegate to this class.
 */
public class PartyService {

    private final MinecraftServer server;
    private final PartySavedData data;

    public PartyService(MinecraftServer server) {
        this.server = server;
        this.data = PartySavedData.get(server);
    }

    /**
     * Gets the PartyService instance for a server.
     */
    public static PartyService get(MinecraftServer server) {
        return new PartyService(server);
    }

    // === Party Creation ===

    public record CreateResult(boolean success, String messageKey, Party party) {}

    /**
     * Creates a new party with the given name.
     */
    public CreateResult createParty(ServerPlayer leader, String name) {
        if (leader == null) {
            return new CreateResult(false, "party.error.invalid_player", null);
        }

        // Check if already in a party
        if (data.isPlayerInParty(leader.getUUID())) {
            return new CreateResult(false, "party.error.already_in_party", null);
        }

        // Check name length
        if (name == null || name.isBlank() || name.length() > 24) {
            return new CreateResult(false, "party.error.invalid_name", null);
        }

        // Check if name is taken
        if (data.findPartyByName(name) != null) {
            return new CreateResult(false, "party.error.name_taken", null);
        }

        // Create the party
        Party party = new Party(name, leader.getUUID());
        data.addParty(party);

        // Sync to client
        syncPartyToMembers(party);

        RagnarMMO.LOGGER.info("Player {} created party '{}'", leader.getName().getString(), name);
        return new CreateResult(true, "party.created", party);
    }

    // === Invites ===

    public enum InviteResult {
        SUCCESS,
        NOT_IN_PARTY,
        NOT_LEADER,
        TARGET_ALREADY_IN_PARTY,
        TARGET_OFFLINE,
        PARTY_FULL,
        ALREADY_INVITED,
        CANNOT_INVITE_SELF
    }

    /**
     * Invites a player to the party.
     */
    public InviteResult invitePlayer(ServerPlayer inviter, ServerPlayer target) {
        if (inviter == null || target == null) {
            return InviteResult.TARGET_OFFLINE;
        }

        if (inviter.getUUID().equals(target.getUUID())) {
            return InviteResult.CANNOT_INVITE_SELF;
        }

        Party party = data.getPartyByPlayer(inviter.getUUID());
        if (party == null) {
            return InviteResult.NOT_IN_PARTY;
        }

        // Only leader can invite (or change this if you want members to invite)
        if (!party.isLeader(inviter.getUUID())) {
            return InviteResult.NOT_LEADER;
        }

        if (party.isFull()) {
            return InviteResult.PARTY_FULL;
        }

        if (data.isPlayerInParty(target.getUUID())) {
            return InviteResult.TARGET_ALREADY_IN_PARTY;
        }

        // Check for existing invite
        PartyInvite existing = data.getInvite(target.getUUID());
        if (existing != null && !existing.isExpired() && existing.getPartyId().equals(party.getPartyId())) {
            return InviteResult.ALREADY_INVITED;
        }

        // Create invite
        PartyInvite invite = new PartyInvite(target.getUUID(), inviter.getUUID(), party.getPartyId());
        data.addInvite(invite);

        // Notify target
        target.sendSystemMessage(Component.translatable("party.invite.received",
                inviter.getName().getString(), party.getName(), invite.getRemainingTimeSeconds()));

        RagnarMMO.LOGGER.debug("Player {} invited {} to party '{}'",
                inviter.getName().getString(), target.getName().getString(), party.getName());

        return InviteResult.SUCCESS;
    }

    // === Accept/Decline Invites ===

    public enum AcceptResult {
        SUCCESS,
        NO_INVITE,
        INVITE_EXPIRED,
        PARTY_NOT_FOUND,
        PARTY_FULL,
        ALREADY_IN_PARTY
    }

    /**
     * Accepts a pending party invite.
     */
    public AcceptResult acceptInvite(ServerPlayer player) {
        if (player == null) {
            return AcceptResult.NO_INVITE;
        }

        // Check if already in party
        if (data.isPlayerInParty(player.getUUID())) {
            data.removeInvite(player.getUUID());
            return AcceptResult.ALREADY_IN_PARTY;
        }

        PartyInvite invite = data.consumeInvite(player.getUUID());
        if (invite == null) {
            return AcceptResult.NO_INVITE;
        }

        if (invite.isExpired()) {
            return AcceptResult.INVITE_EXPIRED;
        }

        Party party = data.getParty(invite.getPartyId());
        if (party == null) {
            return AcceptResult.PARTY_NOT_FOUND;
        }

        if (party.isFull()) {
            return AcceptResult.PARTY_FULL;
        }

        // Add to party
        party.addMember(player.getUUID());
        data.registerPlayerInParty(player.getUUID(), party.getPartyId());
        data.markDirty();

        // Notify all party members
        notifyPartyMembers(party, Component.translatable("party.member.joined", player.getName().getString()));

        // Sync to all members including new one
        syncPartyToMembers(party);

        RagnarMMO.LOGGER.info("Player {} joined party '{}'", player.getName().getString(), party.getName());
        return AcceptResult.SUCCESS;
    }

    /**
     * Declines a pending party invite.
     */
    public boolean declineInvite(ServerPlayer player) {
        if (player == null) return false;

        PartyInvite invite = data.consumeInvite(player.getUUID());
        if (invite == null) return false;

        // Notify inviter if online
        ServerPlayer inviter = server.getPlayerList().getPlayer(invite.getInviterUUID());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("party.invite.declined", player.getName().getString()));
        }

        return true;
    }

    // === Leave Party ===

    public enum LeaveResult {
        SUCCESS,
        NOT_IN_PARTY,
        DISBANDED
    }

    /**
     * Removes a player from their party.
     */
    public LeaveResult leaveParty(ServerPlayer player) {
        if (player == null) return LeaveResult.NOT_IN_PARTY;

        Party party = data.getPartyByPlayer(player.getUUID());
        if (party == null) {
            return LeaveResult.NOT_IN_PARTY;
        }

        UUID oldLeader = party.getLeaderUUID();
        boolean wasLeader = party.isLeader(player.getUUID());

        // Remove from party
        party.removeMember(player.getUUID());
        data.unregisterPlayerFromParty(player.getUUID());

        // Check if party should be disbanded
        if (party.getMemberCount() == 0) {
            data.removeParty(party.getPartyId());
            syncEmptyPartyToPlayer(player);
            RagnarMMO.LOGGER.info("Party '{}' disbanded (last member left)", party.getName());
            return LeaveResult.DISBANDED;
        }

        // Notify remaining members
        notifyPartyMembers(party, Component.translatable("party.member.left", player.getName().getString()));

        // If leader changed, notify
        if (wasLeader && !party.getLeaderUUID().equals(oldLeader)) {
            ServerPlayer newLeader = server.getPlayerList().getPlayer(party.getLeaderUUID());
            if (newLeader != null) {
                notifyPartyMembers(party, Component.translatable("party.leader.promoted", newLeader.getName().getString()));
            }
        }

        data.markDirty();

        // Sync to all affected players
        syncPartyToMembers(party);
        syncEmptyPartyToPlayer(player);

        RagnarMMO.LOGGER.info("Player {} left party '{}'", player.getName().getString(), party.getName());
        return LeaveResult.SUCCESS;
    }

    // === Kick Member ===

    public enum KickResult {
        SUCCESS,
        NOT_IN_PARTY,
        NOT_LEADER,
        TARGET_NOT_IN_PARTY,
        CANNOT_KICK_SELF
    }

    /**
     * Kicks a player from the party (leader only).
     */
    public KickResult kickMember(ServerPlayer leader, UUID targetUUID) {
        if (leader == null || targetUUID == null) {
            return KickResult.TARGET_NOT_IN_PARTY;
        }

        if (leader.getUUID().equals(targetUUID)) {
            return KickResult.CANNOT_KICK_SELF;
        }

        Party party = data.getPartyByPlayer(leader.getUUID());
        if (party == null) {
            return KickResult.NOT_IN_PARTY;
        }

        if (!party.isLeader(leader.getUUID())) {
            return KickResult.NOT_LEADER;
        }

        if (!party.isMember(targetUUID)) {
            return KickResult.TARGET_NOT_IN_PARTY;
        }

        // Remove from party
        party.removeMember(targetUUID);
        data.unregisterPlayerFromParty(targetUUID);
        data.markDirty();

        // Get target player name
        ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
        String targetName = target != null ? target.getName().getString() : targetUUID.toString().substring(0, 8);

        // Notify party
        notifyPartyMembers(party, Component.translatable("party.member.kicked", targetName));

        // Sync to all
        syncPartyToMembers(party);
        if (target != null) {
            syncEmptyPartyToPlayer(target);
            target.sendSystemMessage(Component.translatable("party.you.kicked"));
        }

        RagnarMMO.LOGGER.info("Player {} was kicked from party '{}' by {}",
                targetName, party.getName(), leader.getName().getString());

        return KickResult.SUCCESS;
    }

    // === Promote Leader ===

    public enum PromoteResult {
        SUCCESS,
        NOT_IN_PARTY,
        NOT_LEADER,
        TARGET_NOT_IN_PARTY,
        ALREADY_LEADER
    }

    /**
     * Promotes a member to leader.
     */
    public PromoteResult promoteToLeader(ServerPlayer currentLeader, UUID newLeaderUUID) {
        if (currentLeader == null || newLeaderUUID == null) {
            return PromoteResult.TARGET_NOT_IN_PARTY;
        }

        Party party = data.getPartyByPlayer(currentLeader.getUUID());
        if (party == null) {
            return PromoteResult.NOT_IN_PARTY;
        }

        if (!party.isLeader(currentLeader.getUUID())) {
            return PromoteResult.NOT_LEADER;
        }

        if (!party.isMember(newLeaderUUID)) {
            return PromoteResult.TARGET_NOT_IN_PARTY;
        }

        if (party.isLeader(newLeaderUUID)) {
            return PromoteResult.ALREADY_LEADER;
        }

        party.setLeader(newLeaderUUID);
        data.markDirty();

        ServerPlayer newLeader = server.getPlayerList().getPlayer(newLeaderUUID);
        String newLeaderName = newLeader != null ? newLeader.getName().getString() : newLeaderUUID.toString().substring(0, 8);

        notifyPartyMembers(party, Component.translatable("party.leader.promoted", newLeaderName));
        syncPartyToMembers(party);

        RagnarMMO.LOGGER.info("Player {} promoted {} to leader in party '{}'",
                currentLeader.getName().getString(), newLeaderName, party.getName());

        return PromoteResult.SUCCESS;
    }

    // === Disband Party ===

    public enum DisbandResult {
        SUCCESS,
        NOT_IN_PARTY,
        NOT_LEADER
    }

    /**
     * Disbands the party (leader only).
     */
    public DisbandResult disbandParty(ServerPlayer leader) {
        if (leader == null) return DisbandResult.NOT_IN_PARTY;

        Party party = data.getPartyByPlayer(leader.getUUID());
        if (party == null) {
            return DisbandResult.NOT_IN_PARTY;
        }

        if (!party.isLeader(leader.getUUID())) {
            return DisbandResult.NOT_LEADER;
        }

        String partyName = party.getName();

        // Notify all members before removing
        List<ServerPlayer> onlineMembers = party.getOnlineMembers(server);
        for (ServerPlayer member : onlineMembers) {
            member.sendSystemMessage(Component.translatable("party.disbanded", partyName));
            syncEmptyPartyToPlayer(member);
        }

        // Remove the party
        data.removeParty(party.getPartyId());

        RagnarMMO.LOGGER.info("Party '{}' disbanded by {}", partyName, leader.getName().getString());
        return DisbandResult.SUCCESS;
    }

    // === Queries ===

    public Party getParty(ServerPlayer player) {
        return player == null ? null : data.getPartyByPlayer(player.getUUID());
    }

    public Party getPartyByUUID(UUID partyId) {
        return data.getParty(partyId);
    }

    public Party findPartyByName(String name) {
        return data.findPartyByName(name);
    }

    public boolean isInParty(ServerPlayer player) {
        return player != null && data.isPlayerInParty(player.getUUID());
    }

    public boolean areInSameParty(ServerPlayer p1, ServerPlayer p2) {
        if (p1 == null || p2 == null) return false;
        Party party1 = data.getPartyByPlayer(p1.getUUID());
        Party party2 = data.getPartyByPlayer(p2.getUUID());
        return party1 != null && party1.equals(party2);
    }

    public Collection<Party> getAllParties() {
        return data.getAllParties();
    }

    // === Party Chat ===

    /**
     * Sends a chat message to all online party members.
     */
    public boolean sendPartyChat(ServerPlayer sender, String message) {
        if (sender == null || message == null || message.isBlank()) return false;

        Party party = data.getPartyByPlayer(sender.getUUID());
        if (party == null) return false;

        Component formatted = Component.literal("")
                .append(Component.literal("[Party] ").withStyle(style -> style.withColor(0x55FF55)))
                .append(Component.literal(sender.getName().getString() + ": ").withStyle(style -> style.withColor(0xFFFFFF)))
                .append(Component.literal(message).withStyle(style -> style.withColor(0xAAAAAA)));

        for (ServerPlayer member : party.getOnlineMembers(server)) {
            member.sendSystemMessage(formatted);
        }

        return true;
    }

    // === Sync Helpers ===

    /**
     * Syncs party data to all online members.
     */
    public void syncPartyToMembers(Party party) {
        if (party == null) return;

        for (ServerPlayer member : party.getOnlineMembers(server)) {
            PartySnapshotS2CPacket packet = PartySnapshotS2CPacket.create(party, server);
            Network.sendToPlayer(member, packet);
        }
    }

    /**
     * Syncs one member's live HUD data to all online party members.
     */
    public void syncMemberToMembers(ServerPlayer player) {
        if (player == null) return;

        Party party = data.getPartyByPlayer(player.getUUID());
        if (party == null) return;

        PartyMemberData memberData = PartyMemberData.fromPlayer(player, party.isLeader(player.getUUID()));
        syncMemberToMembers(party, memberData);
    }

    /**
     * Syncs one member's live HUD data to all online party members.
     */
    public void syncMemberToMembers(Party party, PartyMemberData memberData) {
        syncMemberToMembers(party, memberData, null);
    }

    /**
     * Syncs one member's live HUD data to all online party members except one optional UUID.
     */
    public void syncMemberToMembers(Party party, PartyMemberData memberData, UUID excludedMember) {
        if (party == null || memberData == null) return;

        PartyMemberUpdateS2CPacket packet = new PartyMemberUpdateS2CPacket(memberData);
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            if (excludedMember != null && excludedMember.equals(member.getUUID())) {
                continue;
            }
            Network.sendToPlayer(member, packet);
        }
    }

    /**
     * Syncs empty party state to a player (when they leave or are kicked).
     */
    public void syncEmptyPartyToPlayer(ServerPlayer player) {
        if (player == null) return;
        Network.sendToPlayer(player, PartySnapshotS2CPacket.empty());
    }

    /**
     * Notifies all online party members with a message.
     */
    private void notifyPartyMembers(Party party, Component message) {
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            member.sendSystemMessage(message);
        }
    }

    // === Maintenance ===

    /**
     * Cleans up expired invites and invalid parties.
     * Should be called periodically.
     */
    public void cleanup() {
        data.cleanupExpiredInvites();
        data.cleanupInvalidParties();
    }

    /**
     * Called when a player logs in - syncs their party data.
     */
    public void onPlayerLogin(ServerPlayer player) {
        Party party = data.getPartyByPlayer(player.getUUID());
        if (party != null) {
            syncPartyToMembers(party);

            // Notify other members
            for (ServerPlayer member : party.getOnlineMembers(server)) {
                if (!member.getUUID().equals(player.getUUID())) {
                    member.sendSystemMessage(Component.translatable("party.member.online", player.getName().getString()));
                }
            }
        }
    }

    /**
     * Called when a player logs out - notifies party members.
     */
    public void onPlayerLogout(ServerPlayer player) {
        Party party = data.getPartyByPlayer(player.getUUID());
        if (party != null) {
            PartyMemberData offlineData = PartyMemberData.offlineFromPlayer(
                    player, party.isLeader(player.getUUID()));
            syncMemberToMembers(party, offlineData, player.getUUID());

            for (ServerPlayer member : party.getOnlineMembers(server)) {
                if (!member.getUUID().equals(player.getUUID())) {
                    member.sendSystemMessage(Component.translatable("party.member.offline", player.getName().getString()));
                }
            }
        }
    }
}
