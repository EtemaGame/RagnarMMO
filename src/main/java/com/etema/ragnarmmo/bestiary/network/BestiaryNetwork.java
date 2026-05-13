package com.etema.ragnarmmo.bestiary.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public final class BestiaryNetwork {
    private BestiaryNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(SyncBestiaryIndexPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBestiaryIndexPacket::encode)
                .decoder(SyncBestiaryIndexPacket::decode)
                .consumerMainThread(SyncBestiaryIndexPacket::handle)
                .add();

        ch.messageBuilder(RequestBestiaryIndexPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestBestiaryIndexPacket::encode)
                .decoder(RequestBestiaryIndexPacket::decode)
                .consumerMainThread(RequestBestiaryIndexPacket::handle)
                .add();

        ch.messageBuilder(SyncBestiaryDetailsPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBestiaryDetailsPacket::encode)
                .decoder(SyncBestiaryDetailsPacket::decode)
                .consumerMainThread(SyncBestiaryDetailsPacket::handle)
                .add();

        ch.messageBuilder(RequestBestiaryDetailsPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestBestiaryDetailsPacket::encode)
                .decoder(RequestBestiaryDetailsPacket::decode)
                .consumerMainThread(RequestBestiaryDetailsPacket::handle)
                .add();
    }
}
