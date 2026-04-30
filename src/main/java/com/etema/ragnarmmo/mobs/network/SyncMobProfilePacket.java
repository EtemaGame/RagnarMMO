package com.etema.ragnarmmo.mobs.network;

import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobTier;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class SyncMobProfilePacket {
    private final int entityId;
    private final boolean initialized;
    private final MobProfile profile;

    public SyncMobProfilePacket(int entityId, MobProfile profile) {
        this.entityId = entityId;
        this.initialized = profile != null;
        this.profile = profile;
    }

    private SyncMobProfilePacket(int entityId, boolean initialized, MobProfile profile) {
        this.entityId = entityId;
        this.initialized = initialized;
        this.profile = profile;
    }

    public static SyncMobProfilePacket clear(int entityId) {
        return new SyncMobProfilePacket(entityId, false, null);
    }

    public static SyncMobProfilePacket clear(LivingEntity entity) {
        return clear(entity.getId());
    }

    public int entityId() {
        return entityId;
    }

    public boolean initialized() {
        return initialized;
    }

    public Optional<MobProfile> profile() {
        return Optional.ofNullable(profile);
    }

    public static Optional<SyncMobProfilePacket> fromEntity(LivingEntity entity) {
        return MobProfileProvider.get(entity)
                .map(MobProfileState::profile)
                .filter(stateProfile -> MobProfileProvider.get(entity)
                        .map(MobProfileState::isInitialized)
                        .orElse(false))
                .map(profile -> new SyncMobProfilePacket(entity.getId(), profile))
                .stream()
                .findFirst();
    }

    public static void encode(SyncMobProfilePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.initialized);
        if (!msg.initialized) {
            return;
        }
        buf.writeInt(msg.profile.level());
        buf.writeEnum(msg.profile.rank());
        buf.writeEnum(msg.profile.tier());
        buf.writeInt(msg.profile.baseStats().str());
        buf.writeInt(msg.profile.baseStats().agi());
        buf.writeInt(msg.profile.baseStats().vit());
        buf.writeInt(msg.profile.baseStats().intel());
        buf.writeInt(msg.profile.baseStats().dex());
        buf.writeInt(msg.profile.baseStats().luk());
        buf.writeInt(msg.profile.maxHp());
        buf.writeInt(msg.profile.atkMin());
        buf.writeInt(msg.profile.atkMax());
        buf.writeInt(msg.profile.matkMin());
        buf.writeInt(msg.profile.matkMax());
        buf.writeInt(msg.profile.def());
        buf.writeInt(msg.profile.mdef());
        buf.writeInt(msg.profile.hit());
        buf.writeInt(msg.profile.flee());
        buf.writeInt(msg.profile.crit());
        buf.writeInt(msg.profile.aspd());
        buf.writeDouble(msg.profile.moveSpeed());
        buf.writeInt(msg.profile.baseExp());
        buf.writeInt(msg.profile.jobExp());
        buf.writeUtf(msg.profile.race());
        buf.writeUtf(msg.profile.element());
        buf.writeUtf(msg.profile.size());
    }

    public static SyncMobProfilePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        boolean initialized = buf.readBoolean();
        if (!initialized) {
            return clear(entityId);
        }
        return new SyncMobProfilePacket(
                entityId,
                new MobProfile(
                        buf.readInt(),
                        buf.readEnum(com.etema.ragnarmmo.common.api.mobs.MobRank.class),
                        buf.readEnum(MobTier.class),
                        new RoBaseStats(
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt(),
                                buf.readInt()),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readDouble(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readUtf()));
    }

    public static void handle(SyncMobProfilePacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    if (msg.initialized) {
                        com.etema.ragnarmmo.client.ClientPacketHandler.handleMobProfileSync(msg.entityId,
                                msg.profile);
                    } else {
                        com.etema.ragnarmmo.client.ClientPacketHandler.handleMobProfileClear(msg.entityId);
                    }
                }));
        ctx.setPacketHandled(true);
    }
}
