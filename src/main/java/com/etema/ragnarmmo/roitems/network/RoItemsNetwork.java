package com.etema.ragnarmmo.roitems.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers RO Items network packets on the shared channel.
 */
public final class RoItemsNetwork {
    private RoItemsNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(SyncRoItemRulesPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncRoItemRulesPacket::encode)
                .decoder(SyncRoItemRulesPacket::decode)
                .consumerMainThread(SyncRoItemRulesPacket::handle)
                .add();

        ch.messageBuilder(CardCompoundPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(CardCompoundPacket::encode)
                .decoder(CardCompoundPacket::decode)
                .consumerMainThread(CardCompoundPacket::handle)
                .add();
    }
}
