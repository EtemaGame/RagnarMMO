package com.etema.ragnarmmo.items;

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

    public static final RegistryObject<Item> COPPER_ZENY = ITEMS.register("others/zeny/copper_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "item.ragnarmmo.others.zeny.copper_zeny"));

    public static final RegistryObject<Item> SILVER_ZENY = ITEMS.register("others/zeny/silver_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "item.ragnarmmo.others.zeny.silver_zeny"));

    public static final RegistryObject<Item> GOLD_ZENY = ITEMS.register("others/zeny/gold_zeny",
            () -> new ZenyItem(new Item.Properties().stacksTo(64), "item.ragnarmmo.others.zeny.gold_zeny"));

    public static final RegistryObject<Item> MONEY_BAG = ITEMS.register("others/zeny/money_bag",
            () -> new MoneyBagItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static class ZenyItem extends Item {
        private final String translationKey;

        public ZenyItem(Properties properties, String translationKey) {
            super(properties);
            this.translationKey = translationKey;
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                int value = com.etema.ragnarmmo.items.runtime.ZenyWalletHelper.getValue(stack);
                int totalAmount = value * stack.getCount();
                com.etema.ragnarmmo.items.runtime.ZenyWalletHelper.addZeny(serverPlayer, totalAmount);
                stack.setCount(0);
                serverPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f);
                return net.minecraft.world.InteractionResultHolder.consume(stack);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, @javax.annotation.Nonnull List<Component> tooltip, @javax.annotation.Nonnull TooltipFlag flag) {
            tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.GRAY));
            if (stack.is(COPPER_ZENY.get())) {
                tooltip.add(Component.translatable("tooltip.ragnarmmo.zeny.conversion_silver").withStyle(ChatFormatting.DARK_GRAY));
            } else if (stack.is(SILVER_ZENY.get())) {
                tooltip.add(Component.translatable("tooltip.ragnarmmo.zeny.conversion_silver").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.ragnarmmo.zeny.conversion_gold").withStyle(ChatFormatting.DARK_GRAY));
            } else if (stack.is(GOLD_ZENY.get())) {
                tooltip.add(Component.translatable("tooltip.ragnarmmo.zeny.conversion_gold").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private ZenyItems() {
    }
}
