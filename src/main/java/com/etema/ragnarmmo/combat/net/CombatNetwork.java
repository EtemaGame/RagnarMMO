package com.etema.ragnarmmo.combat.net;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Registers combat-architecture packets on the shared channel.
 */
public final class CombatNetwork {
    private CombatNetwork() {
    }

    public static void register(SimpleChannel ch, AtomicInteger id) {
        ch.messageBuilder(ServerboundRagnarBasicAttackPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundRagnarBasicAttackPacket::encode)
                .decoder(ServerboundRagnarBasicAttackPacket::new)
                .consumerMainThread(ServerboundRagnarBasicAttackPacket::handle)
                .add();

        ch.messageBuilder(ServerboundRagnarSkillUsePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundRagnarSkillUsePacket::encode)
                .decoder(ServerboundRagnarSkillUsePacket::new)
                .consumerMainThread(ServerboundRagnarSkillUsePacket::handle)
                .add();

        ch.messageBuilder(ClientboundRagnarCombatResultPacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundRagnarCombatResultPacket::encode)
                .decoder(ClientboundRagnarCombatResultPacket::new)
                .consumerMainThread(ClientboundRagnarCombatResultPacket::handle)
                .add();

        ch.messageBuilder(ClientboundRagnarCastStatePacket.class, id.getAndIncrement(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundRagnarCastStatePacket::encode)
                .decoder(ClientboundRagnarCastStatePacket::new)
                .consumerMainThread(ClientboundRagnarCastStatePacket::handle)
                .add();
    }
}
