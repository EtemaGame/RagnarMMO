package com.etema.ragnarmmo.system.economy.net;

import net.minecraftforge.network.simple.SimpleChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class EconomyNetwork {

    public static void register(SimpleChannel channel, AtomicInteger idGen) {
        channel.messageBuilder(WalletSyncPacket.class, idGen.getAndIncrement())
                .encoder(WalletSyncPacket::encode)
                .decoder(WalletSyncPacket::decode)
                .consumerNetworkThread(WalletSyncPacket::handle)
                .add();

        channel.messageBuilder(ZenyBagActionPacket.class, idGen.getAndIncrement())
                .encoder(ZenyBagActionPacket::encode)
                .decoder(ZenyBagActionPacket::decode)
                .consumerNetworkThread(ZenyBagActionPacket::handle)
                .add();
    }
}
