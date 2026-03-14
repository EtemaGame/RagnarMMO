package com.etema.ragnarmmo.system.stats.net;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers all stats-related network packets on the shared channel.
 */
public final class StatsNetwork {
    private StatsNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(PlayerStatsSyncPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerStatsSyncPacket::encode)
                .decoder(PlayerStatsSyncPacket::decode)
                .consumerMainThread(PlayerStatsSyncPacket::handle)
                .add();

        ch.messageBuilder(AllocateStatPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(AllocateStatPacket::encode)
                .decoder(AllocateStatPacket::decode)
                .consumerMainThread(AllocateStatPacket::handle)
                .add();

        ch.messageBuilder(DeallocateStatPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeallocateStatPacket::encode)
                .decoder(DeallocateStatPacket::decode)
                .consumerMainThread(DeallocateStatPacket::handle)
                .add();

        ch.messageBuilder(ClientboundSkillXpPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundSkillXpPacket::encode)
                .decoder(ClientboundSkillXpPacket::new)
                .consumerMainThread(ClientboundSkillXpPacket::handle)
                .add();

        ch.messageBuilder(ClientboundSkillSyncPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundSkillSyncPacket::encode)
                .decoder(ClientboundSkillSyncPacket::new)
                .consumerMainThread(ClientboundSkillSyncPacket::handle)
                .add();

        ch.messageBuilder(ClientboundLevelUpPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundLevelUpPacket::encode)
                .decoder(ClientboundLevelUpPacket::new)
                .consumerMainThread(ClientboundLevelUpPacket::handle)
                .add();

        ch.messageBuilder(PacketChangeJob.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketChangeJob::toBytes)
                .decoder(PacketChangeJob::new)
                .consumerMainThread(PacketChangeJob::handle)
                .add();

        ch.messageBuilder(PacketResetCharacter.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> msg.encode(buf))
                .decoder(PacketResetCharacter::new)
                .consumerMainThread(PacketResetCharacter::handle)
                .add();

        ch.messageBuilder(PacketUpgradeSkill.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketUpgradeSkill::encode)
                .decoder(PacketUpgradeSkill::new)
                .consumerMainThread(PacketUpgradeSkill::handle)
                .add();

        ch.messageBuilder(DerivedStatsSyncPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DerivedStatsSyncPacket::encode)
                .decoder(DerivedStatsSyncPacket::decode)
                .consumerMainThread(DerivedStatsSyncPacket::handle)
                .add();

        // Skill use packets (logically part of stats/skills)
        ch.messageBuilder(com.etema.ragnarmmo.system.skills.network.PacketUseSkill.class,
                id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.etema.ragnarmmo.system.skills.network.PacketUseSkill::encode)
                .decoder(com.etema.ragnarmmo.system.skills.network.PacketUseSkill::decode)
                .consumerMainThread(com.etema.ragnarmmo.system.skills.network.PacketUseSkill::handle)
                .add();

        ch.messageBuilder(com.etema.ragnarmmo.system.skills.network.PacketSetHotbarSlot.class,
                id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.etema.ragnarmmo.system.skills.network.PacketSetHotbarSlot::encode)
                .decoder(com.etema.ragnarmmo.system.skills.network.PacketSetHotbarSlot::decode)
                .consumerMainThread(com.etema.ragnarmmo.system.skills.network.PacketSetHotbarSlot::handle)
                .add();

        ch.messageBuilder(com.etema.ragnarmmo.system.skills.network.ClientboundCastUpdatePacket.class,
                id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.etema.ragnarmmo.system.skills.network.ClientboundCastUpdatePacket::encode)
                .decoder(com.etema.ragnarmmo.system.skills.network.ClientboundCastUpdatePacket::new)
                .consumerMainThread(com.etema.ragnarmmo.system.skills.network.ClientboundCastUpdatePacket::handle)
                .add();
    }
}
