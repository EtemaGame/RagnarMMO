package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TestItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarCore.MODID);

    // Swords
    public static final RegistryObject<Item> TEST_SWORD_1H = ITEMS.register("test_sword_1h",
            () -> new SwordItem(Tiers.IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> TEST_SWORD_2H = ITEMS.register("test_sword_2h",
            () -> new SwordItem(Tiers.IRON, 6, -3.0F, new Item.Properties()));

    // Dagger
    public static final RegistryObject<Item> TEST_DAGGER = ITEMS.register("test_dagger",
            () -> new SwordItem(Tiers.IRON, 2, -1.6F, new Item.Properties()));

    // Magic
    public static final RegistryObject<Item> TEST_WAND = ITEMS.register("test_wand",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TEST_STAFF = ITEMS.register("test_staff",
            () -> new Item(new Item.Properties()));

    // Shield
    public static final RegistryObject<Item> TEST_SHIELD = ITEMS.register("test_shield",
            () -> new ShieldItem(new Item.Properties().durability(500)));

    // Maces
    public static final RegistryObject<Item> TEST_MACE_1H = ITEMS.register("test_mace_1h",
            () -> new SwordItem(Tiers.IRON, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> TEST_MACE_2H = ITEMS.register("test_mace_2h",
            () -> new SwordItem(Tiers.IRON, 8, -3.2F, new Item.Properties()));

    // Waraxes
    public static final RegistryObject<Item> TEST_WARAXE_1H = ITEMS.register("test_waraxe_1h",
            () -> new AxeItem(Tiers.IRON, 5, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> TEST_WARAXE_2H = ITEMS.register("test_waraxe_2h",
            () -> new AxeItem(Tiers.IRON, 9, -3.4F, new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private TestItems() {
    }
}
