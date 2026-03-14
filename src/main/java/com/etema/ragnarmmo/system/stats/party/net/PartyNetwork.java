package com.etema.ragnarmmo.system.stats.party.net;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers party-related network packets on the shared channel.
 */
public final class PartyNetwork {
    private PartyNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(PartySnapshotS2CPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PartySnapshotS2CPacket::encode)
                .decoder(PartySnapshotS2CPacket::decode)
                .consumerMainThread(PartySnapshotS2CPacket::handle)
                .add();

        ch.messageBuilder(PartyMemberUpdateS2CPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PartyMemberUpdateS2CPacket::encode)
                .decoder(PartyMemberUpdateS2CPacket::decode)
                .consumerMainThread(PartyMemberUpdateS2CPacket::handle)
                .add();
    }
}
