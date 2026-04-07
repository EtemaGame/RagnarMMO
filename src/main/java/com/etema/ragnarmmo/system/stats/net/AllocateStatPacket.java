package com.etema.ragnarmmo.system.stats.net;

import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.system.stats.progression.StatCost;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class AllocateStatPacket {
    public final StatKeys key;

    public AllocateStatPacket(StatKeys k) {
        this.key = k;
    }

    public static void encode(AllocateStatPacket m, FriendlyByteBuf buf) {
        buf.writeEnum(m.key);
    }

    public static AllocateStatPacket decode(FriendlyByteBuf buf) {
        return new AllocateStatPacket(buf.readEnum(StatKeys.class));
    }

    public static void handle(AllocateStatPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ServerPlayer sp = ctx.getSender();
        if (sp == null) {
            ctx.setPacketHandled(true);
            return;
        }
        ctx.enqueueWork(() -> {
            RagnarCoreAPI.get(sp).ifPresent(s -> {
                // Config enforces the playable ceiling; attribute structural bounds stay
                // constant.
                int maxValue = RagnarConfigs.SERVER.caps.maxStatValue.get();
                int currentValue = getStatValue(s, msg.key);
                if (currentValue >= maxValue) {
                    return;
                }

                int cost = StatCost.costToIncrease(currentValue);
                if (s.getStatPoints() < cost) {
                    return;
                }

                switch (msg.key) {
                    case STR -> s.setSTR(currentValue + 1);
                    case AGI -> s.setAGI(currentValue + 1);
                    case VIT -> s.setVIT(currentValue + 1);
                    case INT -> s.setINT(currentValue + 1);
                    case DEX -> s.setDEX(currentValue + 1);
                    case LUK -> s.setLUK(currentValue + 1);
                    default -> throw new IllegalStateException("Unhandled StatKeys: " + msg.key);
                }
                s.setStatPoints(s.getStatPoints() - cost);
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
