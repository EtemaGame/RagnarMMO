package com.etema.ragnarmmo.system.loot.cards;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Generic card item whose identity is determined by an NBT {@code card_id} tag.
 * A single registered item can represent any card definition loaded from JSON.
 * <p>
 * All display data (name, rarity, mob, modifiers) is stored directly in NBT
 * so that tooltips work correctly on the client side even without access to
 * the server-side {@link CardRegistry}.
 */
public class CardItem extends Item {

    public static final String TAG_CARD_ID = "card_id";
    public static final String TAG_CARD_NAME = "card_name";
    public static final String TAG_CARD_MOB = "card_mob";
    public static final String TAG_CARD_MODIFIERS = "card_modifiers";
    public static final String TAG_CARD_DESC = "card_desc";

    public CardItem(Properties props) {
        super(props);
    }

    /* ───────── Factory ───────── */

    /**
     * Creates an ItemStack for the given card definition.
     * Stores all display data in NBT so the client can render tooltips
     * without needing the server-side {@link CardRegistry}.
     */
    public static ItemStack createStack(CardDefinition def) {
        ItemStack stack = new ItemStack(RagnarCardItems.CARD.get());
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(TAG_CARD_ID, def.id());
        tag.putString(TAG_CARD_NAME, def.displayName());
        tag.putString(TAG_CARD_MOB, def.mobId());
        tag.putString(TAG_CARD_DESC, def.translationKey());

        // Store modifiers as a sub-compound for client-side tooltip rendering
        CompoundTag mods = new CompoundTag();
        for (Map.Entry<String, Double> e : def.modifiers().entrySet()) {
            mods.putDouble(e.getKey(), e.getValue());
        }
        tag.put(TAG_CARD_MODIFIERS, mods);

        // Add vanilla custom model data for overlays
        if (def.modelId() > 0) {
            tag.putInt("CustomModelData", def.modelId());
        }

        return stack;
    }

    /* ───────── Interaction ───────── */

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            int slotIndex = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new com.etema.ragnarmmo.client.ui.CardCompoundScreen(slotIndex, stack));
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /* ───────── Tooltip ───────── */

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CARD_ID))
            return;

        String cardName = tag.getString(TAG_CARD_NAME);
        String mobId = tag.getString(TAG_CARD_MOB);

        if (cardName.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ragnarmmo.card.unknown")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Name with standard color for cards
        tooltip.add(Component.literal(cardName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        // Mob source
        tooltip.add(Component.translatable("tooltip.ragnarmmo.card.drops_from",
                formatMobId(mobId))
                .withStyle(ChatFormatting.GRAY));

        // Modifiers
        if (tag.contains(TAG_CARD_MODIFIERS)) {
            CompoundTag mods = tag.getCompound(TAG_CARD_MODIFIERS);
            if (!mods.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.ragnarmmo.card.bonuses")
                        .withStyle(ChatFormatting.DARK_GRAY));
                for (String key : mods.getAllKeys()) {
                    double val = mods.getDouble(key);
                    String attrName = formatAttributeName(key);
                    String sign = val >= 0 ? "+" : "";
                    tooltip.add(Component.literal("  " + sign + (int) val + " " + attrName)
                            .withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(
            @javax.annotation.Nonnull ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CARD_ID)) {
            String cardId = tag.getString(TAG_CARD_ID);
            String mobId = tag.getString(TAG_CARD_MOB);
            String descKey = tag.contains(TAG_CARD_DESC) ? tag.getString(TAG_CARD_DESC) : "";
            return java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>of(
                    new com.etema.ragnarmmo.client.gui.tooltip.CardTooltipData(cardId, mobId, descKey));
        }
        return java.util.Optional.empty();
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CARD_NAME)) {
            String cardName = tag.getString(TAG_CARD_NAME);
            if (!cardName.isEmpty()) {
                return Component.literal(cardName).withStyle(ChatFormatting.YELLOW);
            }
        }
        return super.getName(stack);
    }

    /* ───────── Helpers ───────── */

    @Nullable
    public static String getCardId(ItemStack stack) {
        if (!stack.hasTag())
            return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CARD_ID))
            return null;
        return tag.getString(TAG_CARD_ID);
    }



    /**
     * Formats a mob registry key into Title Case display name.
     * e.g. "minecraft:wither_skeleton" → "Wither Skeleton"
     */
    static String formatMobId(String mobId) {
        if (mobId == null || mobId.isEmpty())
            return "";
        String name = mobId.contains(":") ? mobId.split(":")[1] : mobId;
        String[] words = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                sb.append(' ');
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

    private static String formatAttributeName(String attr) {
        // "ragnarmmo:vit" → "VIT"
        String name = attr.contains(":") ? attr.split(":")[1] : attr;
        return name.toUpperCase();
    }
}
