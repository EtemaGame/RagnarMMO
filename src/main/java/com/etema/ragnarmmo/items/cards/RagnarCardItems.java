package com.etema.ragnarmmo.items.cards;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the single generic card item.
 * Individual card identities are determined by NBT, not by separate Item
 * registrations.
 */
public final class RagnarCardItems {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarMMO.MODID);

    /**
     * The single generic card item. Card identity is stored in NBT via
     * {@code card_id}.
     */
    public static final RegistryObject<Item> CARD = ITEMS.register("cards/card",
            () -> new CardItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private RagnarCardItems() {
    }
}
