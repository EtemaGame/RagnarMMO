package com.etema.ragnarmmo.items;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.init.RagnarEntities;
import com.etema.ragnarmmo.entity.mob.AbstractRagnarMobEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RagnarMobItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarMMO.MODID);

    public static final RegistryObject<Item> PORING_SPAWN_EGG = ITEMS.register("others/eggs/poring_spawn_egg",
            () -> createEgg(RagnarEntities.PORING, 0xDC5F8C, 0xF3B7CB));

    public static final RegistryObject<Item> POPORING_SPAWN_EGG = ITEMS.register("others/eggs/poporing_spawn_egg",
            () -> createEgg(RagnarEntities.POPORING, 0x55A940, 0x9EE07B));

    public static final RegistryObject<Item> DROP_SPAWN_EGG = ITEMS.register("others/eggs/drop_spawn_egg",
            () -> createEgg(RagnarEntities.DROP, 0xE69032, 0xFFD27A));

    public static final RegistryObject<Item> MARIN_SPAWN_EGG = ITEMS.register("others/eggs/marin_spawn_egg",
            () -> createEgg(RagnarEntities.MARIN, 0x3D9CC9, 0x9BDDFA));

    public static final RegistryObject<Item> LUNATIC_SPAWN_EGG = ITEMS.register("others/eggs/lunatic_spawn_egg",
            () -> createEgg(RagnarEntities.LUNATIC, 0xF29ACD, 0xFFF1F7));

    public static final RegistryObject<Item> FABRE_SPAWN_EGG = ITEMS.register("others/eggs/fabre_spawn_egg",
            () -> createEgg(RagnarEntities.FABRE, 0x7CBE5C, 0xC8E79B));

    public static final RegistryObject<Item> PUPA_SPAWN_EGG = ITEMS.register("others/eggs/pupa_spawn_egg",
            () -> createEgg(RagnarEntities.PUPA, 0x9C6B3C, 0xE4C39A));

    public static final RegistryObject<Item> MUKA_SPAWN_EGG = ITEMS.register("others/eggs/muka_spawn_egg",
            () -> createEgg(RagnarEntities.MUKA, 0xC69B5A, 0x6E4B2E));

    public static final RegistryObject<Item> CREAMY_SPAWN_EGG = ITEMS.register("others/eggs/creamy_spawn_egg",
            () -> createEgg(RagnarEntities.CREAMY, 0xFFD27A, 0xDC5F8C));

    public static final RegistryObject<Item> CREAMY_FEAR_SPAWN_EGG = ITEMS.register("others/eggs/creamy_fear_spawn_egg",
            () -> createEgg(RagnarEntities.CREAMY_FEAR, 0x4B0082, 0x000000));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static ForgeSpawnEggItem createEgg(RegistryObject<? extends EntityType<? extends AbstractRagnarMobEntity>> entityType,
                                               int primaryColor, int secondaryColor) {
        return new ForgeSpawnEggItem(entityType, primaryColor, secondaryColor, new Item.Properties());
    }

    private RagnarMobItems() {
    }
}
