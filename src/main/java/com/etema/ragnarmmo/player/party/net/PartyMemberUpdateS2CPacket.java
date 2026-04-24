package com.etema.ragnarmmo.player.party.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Incremental update for a single party member.
 * Sent when: HP changes (throttled), EXP/level changes, leader changes.
 */
public class PartyMemberUpdateS2CPacket {

    private final PartyMemberData memberData;

    public PartyMemberUpdateS2CPacket(PartyMemberData memberData) {
        this.memberData = memberData;
    }

    public PartyMemberData getMemberData() {
        return memberData;
    }

    // === Network Encoding ===

    public void encode(FriendlyByteBuf buf) {
        memberData.encode(buf);
    }

    public static PartyMemberUpdateS2CPacket decode(FriendlyByteBuf buf) {
        return new PartyMemberUpdateS2CPacket(PartyMemberData.decode(buf));
    }

    // === Handler ===

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handlePartyMemberUpdate(memberData));
        });
        ctx.get().setPacketHandled(true);
    }
}
