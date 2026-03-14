package com.etema.ragnarmmo.system.stats.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PlayerStatsSyncPacket {
    public final double mana, manaMax;
    public final double sp, spMax;
    public final int level, exp, statPoints;
    public final int jobLevel, jobExp, skillPoints;
    public final String jobId;
    public final int str, agi, vit, intelligence, dex, luk;

    public PlayerStatsSyncPacket(double mana, double manaMax, double sp, double spMax,
            int level, int exp, int statPoints,
            int jobLevel, int jobExp, int skillPoints, String jobId,
            int str, int agi, int vit, int intelligence, int dex, int luk) {
        this.mana = mana;
        this.manaMax = manaMax;
        this.sp = sp;
        this.spMax = spMax;
        this.level = level;
        this.exp = exp;
        this.statPoints = statPoints;
        this.jobLevel = jobLevel;
        this.jobExp = jobExp;
        this.skillPoints = skillPoints;
        this.jobId = jobId == null ? "" : jobId;
        this.str = str;
        this.agi = agi;
        this.vit = vit;
        this.intelligence = intelligence;
        this.dex = dex;
        this.luk = luk;
    }

    public PlayerStatsSyncPacket(com.etema.ragnarmmo.common.api.stats.IPlayerStats stats) {
        this(stats.getMana(), stats.getManaMax(),
                stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps ? ps.getSP() : 0,
                stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps2 ? ps2.getSPMax() : 100,
                stats.getLevel(), stats.getExp(), stats.getStatPoints(),
                stats.getJobLevel(), stats.getJobExp(), stats.getSkillPoints(), stats.getJobId(),
                stats.getSTR(), stats.getAGI(), stats.getVIT(), stats.getINT(), stats.getDEX(), stats.getLUK());
    }

    public static void encode(PlayerStatsSyncPacket m, FriendlyByteBuf buf) {
        buf.writeDouble(m.mana);
        buf.writeDouble(m.manaMax);
        buf.writeDouble(m.sp);
        buf.writeDouble(m.spMax);
        buf.writeVarInt(m.level);
        buf.writeVarInt(m.exp);
        buf.writeVarInt(m.statPoints);
        buf.writeVarInt(m.jobLevel);
        buf.writeVarInt(m.jobExp);
        buf.writeVarInt(m.skillPoints);
        buf.writeUtf(m.jobId);
        buf.writeVarInt(m.str);
        buf.writeVarInt(m.agi);
        buf.writeVarInt(m.vit);
        buf.writeVarInt(m.intelligence);
        buf.writeVarInt(m.dex);
        buf.writeVarInt(m.luk);
    }

    public static PlayerStatsSyncPacket decode(FriendlyByteBuf buf) {
        return new PlayerStatsSyncPacket(
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PlayerStatsSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handlePlayerStatsSync(msg)));
        ctx.setPacketHandled(true);
    }
}
