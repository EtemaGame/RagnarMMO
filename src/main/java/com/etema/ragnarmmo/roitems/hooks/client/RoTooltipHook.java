package com.etema.ragnarmmo.roitems.hooks.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.config.RoItemsConfig;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds RO-style tooltip information to items.
 * Client-side only.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoTooltipHook {

    private RoTooltipHook() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!RoItemsConfig.isEnabled() || !RoItemsConfig.shouldShowTooltips()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty())
            return;

        // Resolve the rule for this item stack
        RoItemRule rule = RoItemRuleResolver.resolve(stack);

        // Skip if no meaningful rule exists
        if (rule == null || rule.isEmpty())
            return;

        Player player = Minecraft.getInstance().player;

        // Add tooltip lines
        RoTooltipFormatter.addTooltipLines(event.getToolTip(), stack, rule, player);
    }
}
