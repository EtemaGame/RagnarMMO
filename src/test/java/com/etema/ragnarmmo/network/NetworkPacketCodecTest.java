package com.etema.ragnarmmo.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.etema.ragnarmmo.bestiary.network.RequestBestiaryDetailsPacket;
import com.etema.ragnarmmo.economy.zeny.network.WalletSyncPacket;
import com.etema.ragnarmmo.economy.zeny.network.ZenyBagActionPacket;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class NetworkPacketCodecTest {
    @Test
    void walletSyncRoundTripsZenyAmount() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        WalletSyncPacket.encode(new WalletSyncPacket(987_654_321L), buf);

        WalletSyncPacket decoded = WalletSyncPacket.decode(buf);

        assertEquals(987_654_321L, decoded.zeny);
    }

    @Test
    void zenyBagActionRoundTripsEnumContract() throws ReflectiveOperationException {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ZenyBagActionPacket.encode(new ZenyBagActionPacket(ZenyBagActionPacket.Action.WITHDRAW_SILVER), buf);

        ZenyBagActionPacket decoded = ZenyBagActionPacket.decode(buf);

        assertEquals(ZenyBagActionPacket.Action.WITHDRAW_SILVER, readAction(decoded));
    }

    @Test
    void bestiaryDetailsRequestRoundTripsEntityId() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "poring");
        RequestBestiaryDetailsPacket.encode(new RequestBestiaryDetailsPacket(entityId), buf);

        RequestBestiaryDetailsPacket decoded = RequestBestiaryDetailsPacket.decode(buf);

        assertEquals(entityId, decoded.entityId());
    }

    private static ZenyBagActionPacket.Action readAction(ZenyBagActionPacket packet)
            throws ReflectiveOperationException {
        Field field = ZenyBagActionPacket.class.getDeclaredField("action");
        field.setAccessible(true);
        return (ZenyBagActionPacket.Action) field.get(packet);
    }
}
