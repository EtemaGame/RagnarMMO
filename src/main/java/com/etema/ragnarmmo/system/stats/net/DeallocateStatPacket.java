package com.etema.ragnarmmo.system.stats.net;

import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.stats.progression.StatCost;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class DeallocateStatPacket {
    public final StatKeys key;
    public final int amount;

    public DeallocateStatPacket(StatKeys k, int amount) {
        this.key = k;
        this.amount = amount;
    }

    public DeallocateStatPacket(StatKeys k) {
        this(k, 1);
    }

    public static void encode(DeallocateStatPacket m, FriendlyByteBuf buf) {
        buf.writeEnum(m.key);
        buf.writeVarInt(m.amount);
    }

    public static DeallocateStatPacket decode(FriendlyByteBuf buf) {
        return new DeallocateStatPacket(buf.readEnum(StatKeys.class), buf.readVarInt());
    }

    public static void handle(DeallocateStatPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ServerPlayer sp = ctx.getSender();
        if (sp == null) {
            ctx.setPacketHandled(true);
            return;
        }
        ctx.enqueueWork(() -> {
            RagnarCoreAPI.get(sp).ifPresent(s -> {
                for (int i = 0; i < msg.amount; i++) {
                    int currentValue = getStatValue(s, msg.key);
                    if (currentValue <= 1) { // Cannot go below 1
                        break;
                    }

                    int refund = StatCost.costToIncrease(currentValue - 1);

                    switch (msg.key) {
                        case STR -> s.setSTR(currentValue - 1);
                        case AGI -> s.setAGI(currentValue - 1);
                        case VIT -> s.setVIT(currentValue - 1);
                        case INT -> s.setINT(currentValue - 1);
                        case DEX -> s.setDEX(currentValue - 1);
                        case LUK -> s.setLUK(currentValue - 1);
                    }
                    s.setStatPoints(s.getStatPoints() + refund);
                }
            });
        });
        ctx.setPacketHandled(true);
    }

    private static int getStatValue(IPlayerStats stats, StatKeys key) {
        return switch (key) {
            case STR -> stats.getSTR();
            case AGI -> stats.getAGI();
            case VIT -> stats.getVIT();
            case INT -> stats.getINT();
            case DEX -> stats.getDEX();
            case LUK -> stats.getLUK();
            default -> throw new IllegalStateException("Unhandled StatKeys: " + key);
        };
    }
}
