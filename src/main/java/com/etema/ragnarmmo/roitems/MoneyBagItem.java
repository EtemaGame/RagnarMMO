package com.etema.ragnarmmo.roitems;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import javax.annotation.Nullable;
import java.util.List;

public class MoneyBagItem extends Item {

    public MoneyBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            openGui();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    private void openGui() {
        // Triggered via ClientDistributor pattern usually, but let's check how other screens open
        // For now, I'll just set the screen if I can, or use a Proxy/Client-Only event
        com.etema.ragnarmmo.client.gui.MoneyBagOpener.open();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ragnarmmo.zeny.money_bag.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.ragnarmmo.zeny.money_bag.instruction").withStyle(ChatFormatting.GRAY));
    }
}
