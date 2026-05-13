package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.common.init.RagnarEntities;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.entity.mob.AbstractRagnarMobEntity;
import com.etema.ragnarmmo.mobs.spawn.MobSpawnHandler;
import com.etema.ragnarmmo.mobs.util.AttributeLimitHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;

public final class MobsModule {
    private MobsModule() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(MobsModule::onCommonSetup);
        modBus.addListener((EntityAttributeCreationEvent event) -> registerAttributes(event));
        MinecraftForge.EVENT_BUS.register(new MobSpawnHandler());
        Network.registerMobPackets();
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        AttributeLimitHelper.onCommonSetup(event);
    }

    private static void registerAttributes(final EntityAttributeCreationEvent event) {
        for (RegistryObject<? extends net.minecraft.world.entity.EntityType<? extends AbstractRagnarMobEntity>> entityType
                : RagnarEntities.RAGNAR_MOBS) {
            registerAttributes(event, entityType.get());
        }
    }

    private static void registerAttributes(EntityAttributeCreationEvent event,
                                           net.minecraft.world.entity.EntityType<? extends AbstractRagnarMobEntity> entityType) {
        event.put(entityType, AbstractRagnarMobEntity.createAttributes().build());
    }
}
