package com.etema.ragnarmmo.common.api.mobs.runtime.integration;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobCoexistenceViewPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sends the minimal new-source coexistence projection once a covered manual mob is present server-side.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class ManualMobProfileCoexistenceSyncBridge {

    private ManualMobProfileCoexistenceSyncBridge() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) {
            return;
        }

        ManualMobProfileRuntimeStore.get(entity)
                .map(profile -> SyncMobCoexistenceViewPacket.fromProfile(entity.getId(), profile))
                .or(() -> resolveFromEntityType(entity))
                .ifPresent(packet -> Network.sendTrackingEntityAndSelf(entity, packet));
    }

    private static java.util.Optional<SyncMobCoexistenceViewPacket> resolveFromEntityType(LivingEntity entity) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return java.util.Optional.ofNullable(ManualMobProfileResolver.resolve(entityTypeId).profile())
                .map(profile -> SyncMobCoexistenceViewPacket.fromProfile(entity.getId(), profile));
    }
}
