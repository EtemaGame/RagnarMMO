package com.etema.ragnarmmo.roitems.hooks.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.config.access.RoItemsConfigAccess;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.roitems.runtime.RoItemTextHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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
        if (!RoItemsConfigAccess.isEnabled() || !RoItemsConfigAccess.showTooltips()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty())
            return;

        // Resolve the rule for this item stack
        RoItemRule rule = RoItemRuleResolver.resolve(stack);

        // Skip if no meaningful rule exists
        if ((rule == null || rule.isEmpty()) && RoItemNbtHelper.getRefineLevel(stack) <= 0)
            return;

        Player player = Minecraft.getInstance().player;

        if (!event.getToolTip().isEmpty()) {
            event.getToolTip().set(0, RoItemTextHelper.getDisplayName(stack));
        }

        // Add tooltip lines
        RoTooltipFormatter.addTooltipLines(event.getToolTip(), stack, rule, player);

        // Remove vanilla attribute modifier tooltips to avoid redundancy
        hideVanillaAttributes(event.getToolTip());
    }

    private static void hideVanillaAttributes(List<Component> tooltip) {
        // Vanilla attributes start with "When in Main Hand:" or similar headers.
        // We look for common patterns or just clear lines that look like vanilla mods.
        
        // This is a common strategy: removing lines that are added by Item.appendHoverText
        // or automatically by ItemStack.getTooltipLines.
        // Since we can't easily detect "vanilla-ness" of a line after it's added,
        // we look for the "When in..." headers and remove them + their subsequent indented lines.

        for (int i = 0; i < tooltip.size(); i++) {
            Component c = tooltip.get(i);
            String text = c.getString();

            // Check for vanilla slot headers. Weapons use "When in Main Hand";
            // armor uses "When on Head/Body/Legs/Feet".
            if (isVanillaAttributeHeader(text)) {
                // Remove this line and subsequent lines until we hit an empty line or another header
                tooltip.remove(i);
                while (i < tooltip.size()) {
                    Component next = tooltip.get(i);
                    String nextText = next.getString();
                    // Vanilla modifiers are indented with spaces
                    if (nextText.startsWith(" ") || nextText.isEmpty() || isVanillaAttributeModifierLine(nextText)) {
                        tooltip.remove(i);
                    } else {
                        break;
                    }
                }
                i--; // Re-check current index
            }
        }
    }

    private static boolean isVanillaAttributeHeader(String text) {
        return text.startsWith("When in ")
                || text.startsWith("When on ")
                || text.startsWith("Al estar en ");
    }

    private static boolean isVanillaAttributeModifierLine(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return text.startsWith("+") || text.startsWith("-");
    }
}
