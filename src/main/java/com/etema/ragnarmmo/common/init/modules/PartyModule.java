package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.common.net.Network;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PartyModule {
    private PartyModule() {
    }

    public static void init(IEventBus modBus) {
        Network.registerPartyPackets();
    }
}
