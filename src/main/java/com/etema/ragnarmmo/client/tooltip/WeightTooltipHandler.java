package com.etema.ragnarmmo.client.tooltip;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.system.skills.merchant.MerchantSkillEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side tooltip handler that displays item weight.
 * Uses the same weight calculation logic as the server (MerchantSkillEvents).
 *
 * Weight display:
 * - For stackable items: shows weight per full stack
 * - For non-stackable items: shows weight per item
 *
 * Hold SHIFT to see the weight tooltip.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WeightTooltipHandler {

    private static final String WEIGHT_PREFIX = "\u2696 "; // ⚖ balance scale symbol

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        // Calculate display weight (unit weight) and total weight
        double unitWeight = MerchantSkillEvents.computeDisplayWeight(stack);
        double totalWeight = MerchantSkillEvents.computeWeight(stack);

        if (unitWeight <= 0.0D) {
            return;
        }

        // Show hint if SHIFT is not held
        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.ragnarmmo.weight.shift_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        // Format: "⚖ Weight: 0.25 ea. (Total: 16.00)"
        Component weightLine = Component.literal(WEIGHT_PREFIX)
                .append(Component.translatable("tooltip.ragnarmmo.weight.label"))
                .append(Component.literal(": "))
                .append(Component.literal(String.format("%.2f ", unitWeight)))
                .append(Component.translatable("tooltip.ragnarmmo.weight.each"))
                .withStyle(ChatFormatting.GRAY);
        event.getToolTip().add(weightLine);

        // Show total weight if count > 1
        if (stack.getCount() > 1) {
            Component totalLine = Component.literal("  ")
                    .append(Component.translatable("tooltip.ragnarmmo.weight.total",
                            String.format("%.2f", totalWeight)))
                    .withStyle(ChatFormatting.DARK_GRAY);
            event.getToolTip().add(totalLine);
        }
    }
}
