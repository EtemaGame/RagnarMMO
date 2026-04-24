package com.etema.ragnarmmo.mobs.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public final class MobNetwork {
    private MobNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(SyncMobProfilePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMobProfilePacket::encode)
                .decoder(SyncMobProfilePacket::decode)
                .consumerMainThread(SyncMobProfilePacket::handle)
                .add();

        ch.messageBuilder(MobHurtPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobHurtPacket::encode)
                .decoder(MobHurtPacket::decode)
                .consumerMainThread(MobHurtPacket::handle)
                .add();
    }
}
