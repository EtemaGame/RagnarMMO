package com.etema.ragnarmmo.items;

import com.etema.ragnarmmo.items.runtime.RagnarRangedWeaponStats;
import com.etema.ragnarmmo.items.runtime.RoRefineMath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class TooltipTextHelper {

    private TooltipTextHelper() {
    }

    static Component displayName(String fallbackKey, String customName) {
        return customName != null && !customName.isBlank()
                ? Component.literal(customName)
                : Component.translatable(fallbackKey);
    }

    static void appendDescription(List<Component> tooltip, String fallbackKey, String customDescription) {
        if (customDescription != null && !customDescription.isBlank()) {
            tooltip.add(Component.literal(customDescription).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.add(Component.translatable(fallbackKey + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    static void appendRangedStats(List<Component> tooltip, RagnarRangedWeaponStats stats, ItemStack stack) {
        double atk = stats.getRangedWeaponAtk(stack) + RoRefineMath.getAttackBonus(stack);
        int aspd = stats.getBaseRangedAspd(stack);
        int ticks = stats.getBaseDrawTicks(stack);
        
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.ragnarmmo.ranged_stats").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ")
                .append(Component.translatable("tooltip.ragnarmmo.atk"))
                .append(": " + (int)atk)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  ")
                .append(Component.translatable("tooltip.ragnarmmo.aspd"))
                .append(": " + aspd)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  ")
                .append(Component.translatable("tooltip.ragnarmmo.draw_ticks"))
                .append(": " + ticks)
                .withStyle(ChatFormatting.AQUA));
    }
}
