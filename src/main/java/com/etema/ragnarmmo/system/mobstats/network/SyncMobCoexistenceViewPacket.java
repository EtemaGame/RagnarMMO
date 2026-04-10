package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.query.MobClientCoexistenceView;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerDataOrigin;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Syncs the minimal new-source coexistence projection for one tracked mob entity.
 */
public class SyncMobCoexistenceViewPacket {
    private final int entityId;
    private final MobClientCoexistenceView view;

    public SyncMobCoexistenceViewPacket(int entityId, MobClientCoexistenceView view) {
        this.entityId = entityId;
        this.view = view;
    }

    public static SyncMobCoexistenceViewPacket fromProfile(int entityId, ComputedMobProfile profile) {
        return new SyncMobCoexistenceViewPacket(
                entityId,
                new MobClientCoexistenceView(
                        profile.level(),
                        profile.rank(),
                        profile.race(),
                        profile.element(),
                        profile.size(),
                        MobConsumerDataOrigin.NEW_RUNTIME_PROFILE));
    }

    public static Optional<SyncMobCoexistenceViewPacket> fromEntity(LivingEntity entity) {
        Optional<SyncMobCoexistenceViewPacket> resolvedRuntimePacket =
                MobConsumerReadViewResolver.resolve(entity, (com.etema.ragnarmmo.system.mobstats.core.MobStats) null)
                .filter(readView -> readView.dataOrigin() == MobConsumerDataOrigin.NEW_RUNTIME_PROFILE)
                .map(MobClientCoexistenceView::fromReadView)
                .map(view -> new SyncMobCoexistenceViewPacket(entity.getId(), view));
        if (resolvedRuntimePacket.isPresent()) {
            return resolvedRuntimePacket;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return Optional.ofNullable(ManualMobProfileResolver.resolve(entityTypeId).profile())
                .map(profile -> fromProfile(entity.getId(), profile));
    }

    public static void encode(SyncMobCoexistenceViewPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.view.level());
        buf.writeEnum(msg.view.rank());
        buf.writeUtf(msg.view.race());
        buf.writeUtf(msg.view.element());
        buf.writeUtf(msg.view.size());
        buf.writeEnum(msg.view.dataOrigin());
    }

    public static SyncMobCoexistenceViewPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        MobClientCoexistenceView view = new MobClientCoexistenceView(
                buf.readVarInt(),
                buf.readEnum(com.etema.ragnarmmo.common.api.mobs.MobRank.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readEnum(MobConsumerDataOrigin.class));
        return new SyncMobCoexistenceViewPacket(entityId, view);
    }

    public static void handle(SyncMobCoexistenceViewPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleMobCoexistenceViewSync(
                        msg.entityId,
                        msg.view)));
        ctx.setPacketHandled(true);
    }
}
