package com.etema.ragnarmmo.combat.net;

import java.util.function.Supplier;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundRagnarCombatResultPacket {
    private final int attackerId;
    private final int targetId;
    private final CombatHitResultType resultType;
    private final double amount;
    private final boolean critical;

    public ClientboundRagnarCombatResultPacket(int attackerId, int targetId, CombatHitResultType resultType,
            double amount, boolean critical) {
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.resultType = resultType;
        this.amount = amount;
        this.critical = critical;
    }

    public ClientboundRagnarCombatResultPacket(FriendlyByteBuf buf) {
        this.attackerId = buf.readInt();
        this.targetId = buf.readInt();
        this.resultType = CombatHitResultType.values()[buf.readVarInt()];
        this.amount = buf.readDouble();
        this.critical = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(attackerId);
        buf.writeInt(targetId);
        buf.writeVarInt(resultType.ordinal());
        buf.writeDouble(amount);
        buf.writeBoolean(critical);
    }

    public static void handle(ClientboundRagnarCombatResultPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            com.etema.ragnarmmo.client.ClientPacketHandler.handleCombatFeedback(msg.targetId, msg.resultType, msg.amount, msg.critical);
        });
        ctx.setPacketHandled(true);
    }
}
