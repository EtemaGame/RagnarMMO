package com.etema.ragnarmmo.mobs.network;

import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class SyncMobProfilePacket {
    private final int entityId;
    private final MobProfile profile;

    public SyncMobProfilePacket(int entityId, MobProfile profile) {
        this.entityId = entityId;
        this.profile = profile;
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
        buf.writeInt(msg.profile.level());
        buf.writeEnum(msg.profile.rank());
        buf.writeInt(msg.profile.maxHp());
        buf.writeInt(msg.profile.atkMin());
        buf.writeInt(msg.profile.atkMax());
        buf.writeInt(msg.profile.def());
        buf.writeInt(msg.profile.mdef());
        buf.writeInt(msg.profile.hit());
        buf.writeInt(msg.profile.flee());
        buf.writeInt(msg.profile.crit());
        buf.writeInt(msg.profile.aspd());
        buf.writeDouble(msg.profile.moveSpeed());
        buf.writeUtf(msg.profile.race());
        buf.writeUtf(msg.profile.element());
        buf.writeUtf(msg.profile.size());
    }

    public static SyncMobProfilePacket decode(FriendlyByteBuf buf) {
        return new SyncMobProfilePacket(
                buf.readInt(),
                new MobProfile(
                        buf.readInt(),
                        buf.readEnum(com.etema.ragnarmmo.common.api.mobs.MobRank.class),
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
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readUtf()));
    }

    public static void handle(SyncMobProfilePacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleMobProfileSync(msg.entityId,
                        msg.profile)));
        ctx.setPacketHandled(true);
    }
}
