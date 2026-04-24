package com.etema.ragnarmmo.items.runtime;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class RoItemTextHelper {

    private RoItemTextHelper() {
    }

    public static Component getDisplayName(ItemStack stack) {
        return appendRefineSuffix(stack.getHoverName().copy(), stack);
    }

    public static Component appendRefineSuffix(Component baseName, ItemStack stack) {
        int refineLevel = RoItemNbtHelper.getRefineLevel(stack);
        if (refineLevel <= 0) {
            return baseName;
        }

        return baseName.copy()
                .append(Component.literal(" +" + refineLevel).withStyle(ChatFormatting.AQUA));
    }

    public static String getDisplayNameString(ItemStack stack) {
        return getDisplayName(stack).getString();
    }
}
