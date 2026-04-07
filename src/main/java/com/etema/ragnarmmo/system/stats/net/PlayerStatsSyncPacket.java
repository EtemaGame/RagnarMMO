package com.etema.ragnarmmo.system.stats.net;

import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PlayerStatsSyncPacket {
    public final int syncMask;
    public final double mana, manaMax;
    public final double sp, spMax;
    public final int level, exp, statPoints;
    public final int jobLevel, jobExp, skillPoints;
    public final String jobId;
    public final int str, agi, vit, intelligence, dex, luk;
    
    // Auth derived stats for HUD
    public final double physAtkMin, physAtkMax, magicAtk;
    public final double accuracy, flee, critChance;
    public final double aspd, castTime;

    public PlayerStatsSyncPacket(int syncMask,
            double mana, double manaMax, double sp, double spMax,
            int level, int exp, int statPoints,
            int jobLevel, int jobExp, int skillPoints, String jobId,
            int str, int agi, int vit, int intelligence, int dex, int luk,
            double physAtkMin, double physAtkMax, double magicAtk,
            double accuracy, double flee, double critChance,
            double aspd, double castTime) {
        this.syncMask = syncMask;
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
        this.physAtkMin = physAtkMin;
        this.physAtkMax = physAtkMax;
        this.magicAtk = magicAtk;
        this.accuracy = accuracy;
        this.flee = flee;
        this.critChance = critChance;
        this.aspd = aspd;
        this.castTime = castTime;
    }

    public PlayerStatsSyncPacket(com.etema.ragnarmmo.common.api.stats.IPlayerStats stats) {
        this(stats, RoPlayerSyncDomain.allMask());
    }

    public PlayerStatsSyncPacket(com.etema.ragnarmmo.common.api.stats.IPlayerStats stats, int syncMask) {
        this(syncMask,
                stats.getMana(), stats.getManaMax(),
                stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps ? ps.getSP() : 0,
                stats instanceof com.etema.ragnarmmo.system.stats.capability.PlayerStats ps2 ? ps2.getSPMax() : 100,
                stats.getLevel(), stats.getExp(), stats.getStatPoints(),
                stats.getJobLevel(), stats.getJobExp(), stats.getSkillPoints(), stats.getJobId(),
                stats.getSTR(), stats.getAGI(), stats.getVIT(), stats.getINT(), stats.getDEX(), stats.getLUK(),
                0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static void encode(PlayerStatsSyncPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.syncMask);
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
        buf.writeDouble(m.physAtkMin);
        buf.writeDouble(m.physAtkMax);
        buf.writeDouble(m.magicAtk);
        buf.writeDouble(m.accuracy);
        buf.writeDouble(m.flee);
        buf.writeDouble(m.critChance);
        buf.writeDouble(m.aspd);
        buf.writeDouble(m.castTime);
    }

    public static PlayerStatsSyncPacket decode(FriendlyByteBuf buf) {
        return new PlayerStatsSyncPacket(
                buf.readVarInt(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble());
    }

    public static void handle(PlayerStatsSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handlePlayerStatsSync(msg)));
        ctx.setPacketHandled(true);
    }
}
