package com.etema.ragnarmmo.skills.net;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers all skill-related network packets on the shared channel.
 */
public final class SkillsNetwork {
    private SkillsNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(PacketUseSkill.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketUseSkill::encode)
                .decoder(PacketUseSkill::new)
                .consumerMainThread(PacketUseSkill::handle)
                .add();

        ch.messageBuilder(PacketSetHotbarSlot.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketSetHotbarSlot::encode)
                .decoder(PacketSetHotbarSlot::new)
                .consumerMainThread(PacketSetHotbarSlot::handle)
                .add();

        ch.messageBuilder(ClientboundCastUpdatePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundCastUpdatePacket::encode)
                .decoder(ClientboundCastUpdatePacket::new)
                .consumerMainThread(ClientboundCastUpdatePacket::handle)
                .add();

        ch.messageBuilder(SyncSkillDefinitionsPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillDefinitionsPacket::encode)
                .decoder(SyncSkillDefinitionsPacket::new)
                .consumerMainThread(SyncSkillDefinitionsPacket::handle)
                .add();

        ch.messageBuilder(SyncSkillTreesPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillTreesPacket::encode)
                .decoder(SyncSkillTreesPacket::new)
                .consumerMainThread(SyncSkillTreesPacket::handle)
                .add();
    }
}
