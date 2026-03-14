package com.etema.ragnarmmo.system.stats.party.net;

import com.etema.ragnarmmo.system.stats.party.Party;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Full snapshot of party data sent to client.
 * Sent on: join server, join/leave party, disband, accept invite.
 */
public class PartySnapshotS2CPacket {

    private final boolean hasParty;
    private final UUID partyId;
    private final String partyName;
    private final List<PartyMemberData> members;

    public PartySnapshotS2CPacket(boolean hasParty, UUID partyId, String partyName, List<PartyMemberData> members) {
        this.hasParty = hasParty;
        this.partyId = partyId;
        this.partyName = partyName;
        this.members = members != null ? members : new ArrayList<>();
    }

    /**
     * Creates an empty snapshot (player not in party).
     */
    public static PartySnapshotS2CPacket empty() {
        return new PartySnapshotS2CPacket(false, null, "", new ArrayList<>());
    }

    /**
     * Creates a snapshot from a party on the server.
     */
    public static PartySnapshotS2CPacket create(Party party, MinecraftServer server) {
        if (party == null) {
            return empty();
        }

        List<PartyMemberData> memberDataList = new ArrayList<>();

        for (UUID memberUUID : party.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberUUID);
            boolean isLeader = party.isLeader(memberUUID);

            if (player != null) {
                memberDataList.add(PartyMemberData.fromPlayer(player, isLeader));
            } else {
                memberDataList.add(PartyMemberData.offline(memberUUID, isLeader));
            }
        }

        return new PartySnapshotS2CPacket(true, party.getPartyId(), party.getName(), memberDataList);
    }

    // === Getters ===

    public boolean hasParty() {
        return hasParty;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public List<PartyMemberData> getMembers() {
        return members;
    }

    // === Network Encoding ===

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasParty);

        if (hasParty) {
            buf.writeUUID(partyId);
            buf.writeUtf(partyName, 64);
            buf.writeVarInt(members.size());

            for (PartyMemberData member : members) {
                member.encode(buf);
            }
        }
    }

    public static PartySnapshotS2CPacket decode(FriendlyByteBuf buf) {
        boolean hasParty = buf.readBoolean();

        if (!hasParty) {
            return empty();
        }

        UUID partyId = buf.readUUID();
        String partyName = buf.readUtf(64);
        int memberCount = buf.readVarInt();

        List<PartyMemberData> members = new ArrayList<>(memberCount);
        for (int i = 0; i < memberCount; i++) {
            members.add(PartyMemberData.decode(buf));
        }

        return new PartySnapshotS2CPacket(true, partyId, partyName, members);
    }

    // === Handler ===

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handlePartySnapshot(
                            hasParty, partyId, partyName, members));
        });
        ctx.get().setPacketHandled(true);
    }
}
