package com.etema.ragnarmmo.mobs.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class SyncMobProfilePacketTest {
    @Test
    void clearPacketDecodesWithoutMobProfilePayload() {
        SyncMobProfilePacket packet = SyncMobProfilePacket.clear(42);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());

        SyncMobProfilePacket.encode(packet, encoded);
        SyncMobProfilePacket decoded = SyncMobProfilePacket.decode(encoded);

        assertEquals(42, decoded.entityId());
        assertFalse(decoded.initialized());
        assertFalse(decoded.profile().isPresent());
    }
}
