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

    public DeallocateStatPacket(StatKeys k) {
        this.key = k;
    }

    public static void encode(DeallocateStatPacket m, FriendlyByteBuf buf) {
        buf.writeEnum(m.key);
    }

    public static DeallocateStatPacket decode(FriendlyByteBuf buf) {
        return new DeallocateStatPacket(buf.readEnum(StatKeys.class));
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
                int currentValue = getStatValue(s, msg.key);
                if (currentValue <= 1) { // Cannot go below 1
                    return;
                }

                // Cost to reach current level from previous level IS the refund amount
                // costToIncrease(current - 1) gives cost to go from (current-1) -> current.
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
                s.markDirty();
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
        };
    }
}






