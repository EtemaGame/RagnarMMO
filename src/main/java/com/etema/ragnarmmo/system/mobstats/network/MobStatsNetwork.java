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


        ch.messageBuilder(ManualMobCatalogRequestPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManualMobCatalogRequestPacket::encode)
                .decoder(ManualMobCatalogRequestPacket::decode)
                .consumerMainThread(ManualMobCatalogRequestPacket::handle)
                .add();

        ch.messageBuilder(ManualMobCatalogResponsePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManualMobCatalogResponsePacket::encode)
                .decoder(ManualMobCatalogResponsePacket::decode)
                .consumerMainThread(ManualMobCatalogResponsePacket::handle)
                .add();

        ch.messageBuilder(ManualMobDetailRequestPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManualMobDetailRequestPacket::encode)
                .decoder(ManualMobDetailRequestPacket::decode)
                .consumerMainThread(ManualMobDetailRequestPacket::handle)
                .add();

        ch.messageBuilder(ManualMobDetailResponsePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManualMobDetailResponsePacket::encode)
                .decoder(ManualMobDetailResponsePacket::decode)
                .consumerMainThread(ManualMobDetailResponsePacket::handle)
                .add();

        ch.messageBuilder(ManualMobSaveEntryPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManualMobSaveEntryPacket::encode)
                .decoder(ManualMobSaveEntryPacket::decode)
                .consumerMainThread(ManualMobSaveEntryPacket::handle)
                .add();

        ch.messageBuilder(ManualMobDeleteEntryPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManualMobDeleteEntryPacket::encode)
                .decoder(ManualMobDeleteEntryPacket::decode)
                .consumerMainThread(ManualMobDeleteEntryPacket::handle)
                .add();
    }
}
