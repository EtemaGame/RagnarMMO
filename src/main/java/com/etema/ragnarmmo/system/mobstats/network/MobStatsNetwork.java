package com.etema.ragnarmmo.system.mobstats.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers mob stats network packets on the shared channel.
 */
public final class MobStatsNetwork {
    private MobStatsNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(SyncMobStatsPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMobStatsPacket::encode)
                .decoder(SyncMobStatsPacket::decode)
                .consumerMainThread(SyncMobStatsPacket::handle)
                .add();

        ch.messageBuilder(SyncMobCoexistenceViewPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMobCoexistenceViewPacket::encode)
                .decoder(SyncMobCoexistenceViewPacket::decode)
                .consumerMainThread(SyncMobCoexistenceViewPacket::handle)
                .add();

        ch.messageBuilder(MobHurtPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobHurtPacket::encode)
                .decoder(MobHurtPacket::decode)
                .consumerMainThread(MobHurtPacket::handle)
                .add();
    }
}
