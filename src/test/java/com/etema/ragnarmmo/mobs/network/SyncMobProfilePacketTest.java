package com.etema.ragnarmmo.mobs.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobTier;
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

    @Test
    void profilePacketRoundTripsTierAndRewards() {
        MobProfile profile = new MobProfile(12, MobRank.ELITE, MobTier.ELITE, 240, 18, 27, 8, 5, 42, 33,
                4, 155, 0.25D, 120, 78, "brute", "fire", "large");
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());

        SyncMobProfilePacket.encode(new SyncMobProfilePacket(7, profile), encoded);
        SyncMobProfilePacket decoded = SyncMobProfilePacket.decode(encoded);

        assertTrue(decoded.initialized());
        MobProfile decodedProfile = decoded.profile().orElseThrow();
        assertEquals(MobTier.ELITE, decodedProfile.tier());
        assertEquals(120, decodedProfile.baseExp());
        assertEquals(78, decodedProfile.jobExp());
    }
}
