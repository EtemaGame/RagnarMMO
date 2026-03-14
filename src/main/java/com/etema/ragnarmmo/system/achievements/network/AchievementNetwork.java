package com.etema.ragnarmmo.system.achievements.network;

import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public class AchievementNetwork {
    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(SyncAchievementsPacket.class, id.getAndIncrement())
                .encoder(SyncAchievementsPacket::encode)
                .decoder(SyncAchievementsPacket::decode)
                .consumerMainThread(SyncAchievementsPacket::handle)
                .add();

        ch.messageBuilder(ClaimAchievementPacket.class, id.getAndIncrement())
                .encoder(ClaimAchievementPacket::encode)
                .decoder(ClaimAchievementPacket::decode)
                .consumerMainThread(ClaimAchievementPacket::handle)
                .add();

        ch.messageBuilder(SetTitlePacket.class, id.getAndIncrement())
                .encoder(SetTitlePacket::encode)
                .decoder(SetTitlePacket::decode)
                .consumerMainThread(SetTitlePacket::handle)
                .add();
    }
}
