package com.etema.ragnarmmo.items;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DaggerItems {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        RagnarCore.MODID);

        // Daggers
        public static final RegistryObject<Item> GLACIAL_SHARD = ITEMS.register("daggers/glacial_shard",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> VOLCANIC_TOOTH = ITEMS.register("daggers/volcanic_tooth",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> GALE_DIRK = ITEMS.register("daggers/gale_dirk",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> QUAKE_STILETTO = ITEMS.register("daggers/quake_stiletto",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> VEIN_SEEKER = ITEMS.register("daggers/vein_seeker",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> SHADOWS_EMBRACE = ITEMS.register("daggers/shadows_embrace",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> MAMBAS_KISS = ITEMS.register("daggers/mambas_kiss",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> GHOSTLY_STING = ITEMS.register("daggers/ghostly_sting",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> VALKYRIES_THORN = ITEMS.register("daggers/valkyries_thorn",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> ABYSSAL_FANG = ITEMS.register("daggers/abyssal_fang",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> TWILIGHT_CARVER = ITEMS.register("daggers/twilight_carver",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));
        public static final RegistryObject<Item> OATHBREAKER = ITEMS.register("daggers/oathbreaker",
                        () -> new RagnarDaggerItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));

        public static void register(IEventBus modBus) {
                ITEMS.register(modBus);
        }
}
