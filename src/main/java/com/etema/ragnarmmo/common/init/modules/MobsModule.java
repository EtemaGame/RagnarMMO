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
        registerAttributes(event, RagnarEntities.PORING.get());
        registerAttributes(event, RagnarEntities.POPORING.get());
        registerAttributes(event, RagnarEntities.DROP.get());
        registerAttributes(event, RagnarEntities.MARIN.get());
        registerAttributes(event, RagnarEntities.LUNATIC.get());
        registerAttributes(event, RagnarEntities.FABRE.get());
        registerAttributes(event, RagnarEntities.PUPA.get());
        registerAttributes(event, RagnarEntities.MUKA.get());
        registerAttributes(event, RagnarEntities.CREAMY.get());
        registerAttributes(event, RagnarEntities.CREAMY_FEAR.get());
    }

    private static void registerAttributes(EntityAttributeCreationEvent event,
                                           net.minecraft.world.entity.EntityType<? extends AbstractRagnarMobEntity> entityType) {
        event.put(entityType, AbstractRagnarMobEntity.createAttributes().build());
    }
}
