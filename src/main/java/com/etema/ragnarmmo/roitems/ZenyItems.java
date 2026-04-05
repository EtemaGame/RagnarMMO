package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public final class ZenyItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RagnarCore.MODID);

    public static final RegistryObject<Item> COPPER_ZENY = ITEMS.register("zeny/copper_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "§6Copper Zeny"));

    public static final RegistryObject<Item> SILVER_ZENY = ITEMS.register("zeny/silver_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "§7Silver Zeny"));

    public static final RegistryObject<Item> GOLD_ZENY = ITEMS.register("zeny/gold_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "§eGold Zeny"));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static class ZenyItem extends Item {
        private final String displayName;

        public ZenyItem(Properties properties, String displayName) {
            super(properties);
            this.displayName = displayName;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, @javax.annotation.Nonnull List<Component> tooltip, @javax.annotation.Nonnull TooltipFlag flag) {
            tooltip.add(Component.literal(displayName).withStyle(ChatFormatting.GRAY));
            if (stack.is(COPPER_ZENY.get())) {
                tooltip.add(Component.literal("§79 Copper = 1 Silver").withStyle(ChatFormatting.DARK_GRAY));
            } else if (stack.is(SILVER_ZENY.get())) {
                tooltip.add(Component.literal("§79 Copper = 1 Silver").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("§e9 Silver = 1 Gold").withStyle(ChatFormatting.DARK_GRAY));
            } else if (stack.is(GOLD_ZENY.get())) {
                tooltip.add(Component.literal("§e9 Silver = 1 Gold").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private ZenyItems() {
    }
}
