package com.etema.ragnarmmo.system.lifeskills;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers life skills network packets on the shared channel.
 */
public final class LifeSkillsNetwork {
    private LifeSkillsNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(LifeSkillSyncPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LifeSkillSyncPacket::encode)
                .decoder(LifeSkillSyncPacket::new)
                .consumerMainThread(LifeSkillSyncPacket::handle)
                .add();

        ch.messageBuilder(LifeSkillUpdatePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LifeSkillUpdatePacket::encode)
                .decoder(LifeSkillUpdatePacket::new)
                .consumerMainThread(LifeSkillUpdatePacket::handle)
                .add();

        ch.messageBuilder(LifeSkillPointsPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LifeSkillPointsPacket::encode)
                .decoder(LifeSkillPointsPacket::new)
                .consumerMainThread(LifeSkillPointsPacket::handle)
                .add();

        ch.messageBuilder(LifeSkillLevelUpPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LifeSkillLevelUpPacket::encode)
                .decoder(LifeSkillLevelUpPacket::new)
                .consumerMainThread(LifeSkillLevelUpPacket::handle)
                .add();

        ch.messageBuilder(LifeSkillPerkChoicePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LifeSkillPerkChoicePacket::encode)
                .decoder(LifeSkillPerkChoicePacket::new)
                .consumerMainThread(LifeSkillPerkChoicePacket::handle)
                .add();
    }
}
