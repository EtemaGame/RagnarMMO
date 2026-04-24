package com.etema.ragnarmmo.items;

import com.etema.ragnarmmo.common.init.RagnarCore;
import com.etema.ragnarmmo.mobs.world.BossSigilItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class UtilityItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarCore.MODID);

    public static final RegistryObject<Item> BLUE_GEMSTONE = ITEMS.register("utility/blue_gemstone",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> ORIDECON = ITEMS.register("utility/oridecon",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> ELUNIUM = ITEMS.register("utility/elunium",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BOSS_SIGIL = ITEMS.register("utility/boss_sigil",
            () -> new BossSigilItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static boolean consumeBlueGemstone(ServerPlayer player) {
        return consumeItem(player, BLUE_GEMSTONE.get(), 1);
    }

    public static int countItem(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    public static boolean consumeItem(ServerPlayer player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countItem(player, item) < amount) {
            return false;
        }

        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), amount,
                player.inventoryMenu.getCraftSlots());
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    private UtilityItems() {
    }
}
