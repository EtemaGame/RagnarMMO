package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;

public class SyncMobStatsPacket {
    private final int entityId;
    private final int level;
    private final MobTier tier;
    private final double hpMult, dmgMult, defMult, spdMult;

    public SyncMobStatsPacket(int entityId, MobStats stats) {
        this.entityId = entityId;
        this.level = stats.getLevel();
        this.tier = stats.getTier();
        this.hpMult = stats.getHealthMultiplier();
        this.dmgMult = stats.getDamageMultiplier();
        this.defMult = stats.getDefenseMultiplier();
        this.spdMult = stats.getSpeedMultiplier();
    }

    public static Optional<SyncMobStatsPacket> fromEntity(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return MobStatsProvider.get(entity)
                .filter(MobStats::isInitialized)
                .map(stats -> new SyncMobStatsPacket(entity.getId(), stats));
    }

    public static void encode(SyncMobStatsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.level);
        buf.writeEnum(msg.tier);
        buf.writeDouble(msg.hpMult);
        buf.writeDouble(msg.dmgMult);
        buf.writeDouble(msg.defMult);
        buf.writeDouble(msg.spdMult);
    }

    public static SyncMobStatsPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        int level = buf.readInt();
        MobTier tier = buf.readEnum(MobTier.class);
        double hp = buf.readDouble();
        double dmg = buf.readDouble();
        double def = buf.readDouble();
        double spd = buf.readDouble();
        return new SyncMobStatsPacket(id, level, tier, hp, dmg, def, spd);
    }

    private SyncMobStatsPacket(int entityId, int level, MobTier tier,
            double hpMult, double dmgMult, double defMult, double spdMult) {
        this.entityId = entityId;
        this.level = level;
        this.tier = tier;
        this.hpMult = hpMult;
        this.dmgMult = dmgMult;
        this.defMult = defMult;
        this.spdMult = spdMult;
    }

    public static void handle(SyncMobStatsPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleMobStatsSync(
                        msg.entityId, msg.level, msg.tier,
                        msg.hpMult, msg.dmgMult, msg.defMult, msg.spdMult)));
        ctx.setPacketHandled(true);
    }
}
