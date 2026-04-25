package com.etema.ragnarmmo.items.cards;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generic card item whose identity is determined by an NBT {@code card_id} tag.
 * A single registered item can represent any card definition loaded from JSON.
 *
 * <p>All display data is stored directly in NBT so the client can render
 * correct names and bonuses without depending on the server-side registry.</p>
 */
public class CardItem extends Item {

    public static final String TAG_CARD_ID = "card_id";
    public static final String TAG_CARD_NAME = "card_name";
    public static final String TAG_CARD_MOB = "card_mob";
    public static final String TAG_CARD_MODIFIERS = "card_modifiers";
    public static final String TAG_CARD_EQUIP_TYPE = "card_equip_type";
    public static final String TAG_CARD_DESC = "card_desc";

    public CardItem(Properties props) {
        super(props);
    }

    public static ItemStack createStack(CardDefinition def) {
        ItemStack stack = new ItemStack(RagnarCardItems.CARD.get());
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(TAG_CARD_ID, def.id());
        tag.putString(TAG_CARD_NAME, def.displayName());
        tag.putString(TAG_CARD_MOB, def.mobId());
        tag.putString(TAG_CARD_EQUIP_TYPE, def.equipType().name());
        tag.putString(TAG_CARD_DESC, def.translationKey());

        CompoundTag mods = new CompoundTag();
        for (Map.Entry<String, Double> entry : def.modifiers().entrySet()) {
            mods.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_CARD_MODIFIERS, mods);

        return stack;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            int slotIndex = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.openCardCompoundScreen(slotIndex, stack));
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CARD_ID)) {
            return;
        }

        String cardName = tag.getString(TAG_CARD_NAME);
        String mobId = tag.getString(TAG_CARD_MOB);

        if (cardName.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ragnarmmo.card.unknown")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.literal(cardName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.ragnarmmo.card.drops_from", formatMobId(mobId))
                .withStyle(ChatFormatting.GRAY));
        if (tag.contains(TAG_CARD_EQUIP_TYPE)) {
            CardEquipType equipType = CardEquipType.fromString(tag.getString(TAG_CARD_EQUIP_TYPE));
            if (equipType != CardEquipType.ANY) {
                tooltip.add(Component.literal("For: " + equipType.displayName()).withStyle(ChatFormatting.BLUE));
            }
        }

        if (!tag.contains(TAG_CARD_MODIFIERS)) {
            return;
        }

        CompoundTag mods = tag.getCompound(TAG_CARD_MODIFIERS);
        if (mods.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("tooltip.ragnarmmo.card.bonuses")
                .withStyle(ChatFormatting.DARK_GRAY));
        for (String key : mods.getAllKeys()) {
            double value = mods.getDouble(key);
            tooltip.add(Component.literal("  " + formatModifierValue(key, value) + " " + formatAttributeName(key))
                    .withStyle(ChatFormatting.GREEN));
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
            return java.util.Optional.of(
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

    @Nullable
    public static String getCardId(ItemStack stack) {
        if (!stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CARD_ID)) {
            return null;
        }
        return tag.getString(TAG_CARD_ID);
    }

    public static CardEquipType getCardEquipType(ItemStack stack) {
        if (!stack.hasTag()) {
            return CardEquipType.ANY;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CARD_EQUIP_TYPE)) {
            return CardEquipType.ANY;
        }
        return CardEquipType.fromString(tag.getString(TAG_CARD_EQUIP_TYPE));
    }

    static String formatMobId(String mobId) {
        if (mobId == null || mobId.isEmpty()) {
            return "";
        }
        String name = mobId.contains(":") ? mobId.split(":")[1] : mobId;
        String[] words = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            if (!words[i].isEmpty()) {
                builder.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    builder.append(words[i].substring(1));
                }
            }
        }
        return builder.toString();
    }

    private static String formatAttributeName(String attr) {
        String name = attr.contains(":") ? attr.split(":")[1] : attr;
        if (name.startsWith("damage_vs_race_")) {
            return "Damage vs " + formatCategoryName(name.substring("damage_vs_race_".length()));
        }
        if (name.startsWith("damage_vs_element_")) {
            return "Damage vs " + formatCategoryName(name.substring("damage_vs_element_".length()));
        }
        if (name.startsWith("damage_vs_size_")) {
            return "Damage vs " + formatCategoryName(name.substring("damage_vs_size_".length()));
        }
        if (name.startsWith("physical_damage_vs_race_")) {
            return "Physical damage vs " + formatCategoryName(name.substring("physical_damage_vs_race_".length()));
        }
        if (name.startsWith("physical_damage_vs_element_")) {
            return "Physical damage vs " + formatCategoryName(name.substring("physical_damage_vs_element_".length()));
        }
        if (name.startsWith("physical_damage_vs_size_")) {
            return "Physical damage vs " + formatCategoryName(name.substring("physical_damage_vs_size_".length()));
        }
        if (name.startsWith("magic_damage_vs_race_")) {
            return "Magic damage vs " + formatCategoryName(name.substring("magic_damage_vs_race_".length()));
        }
        if (name.startsWith("magic_damage_vs_element_")) {
            return "Magic damage vs " + formatCategoryName(name.substring("magic_damage_vs_element_".length()));
        }
        if (name.startsWith("magic_damage_vs_size_")) {
            return "Magic damage vs " + formatCategoryName(name.substring("magic_damage_vs_size_".length()));
        }
        if (name.startsWith("resist_race_")) {
            return "Resist " + formatCategoryName(name.substring("resist_race_".length()));
        }
        if (name.startsWith("resist_element_")) {
            return "Resist " + formatCategoryName(name.substring("resist_element_".length()));
        }
        if (name.startsWith("resist_size_")) {
            return "Resist " + formatCategoryName(name.substring("resist_size_".length()));
        }
        if (name.startsWith("physical_resist_race_")) {
            return "Physical resist " + formatCategoryName(name.substring("physical_resist_race_".length()));
        }
        if (name.startsWith("physical_resist_element_")) {
            return "Physical resist " + formatCategoryName(name.substring("physical_resist_element_".length()));
        }
        if (name.startsWith("physical_resist_size_")) {
            return "Physical resist " + formatCategoryName(name.substring("physical_resist_size_".length()));
        }
        if (name.startsWith("magic_resist_race_")) {
            return "Magic resist " + formatCategoryName(name.substring("magic_resist_race_".length()));
        }
        if (name.startsWith("magic_resist_element_")) {
            return "Magic resist " + formatCategoryName(name.substring("magic_resist_element_".length()));
        }
        if (name.startsWith("magic_resist_size_")) {
            return "Magic resist " + formatCategoryName(name.substring("magic_resist_size_".length()));
        }
        if ("damage_vs_boss".equals(name)) {
            return "Damage vs Boss";
        }
        if ("resist_boss".equals(name)) {
            return "Resist Boss";
        }
        return name.toUpperCase(Locale.ROOT);
    }

    private static String formatModifierValue(String attr, double value) {
        String name = attr.contains(":") ? attr.split(":")[1] : attr;
        boolean percent = name.contains("damage_vs_") || name.contains("resist_");
        if (percent) {
            int display = (int) Math.round(value * 100.0);
            return (display >= 0 ? "+" : "") + display + "%";
        }

        if (Math.abs(value - Math.rint(value)) < 1.0e-6) {
            int display = (int) Math.round(value);
            return (display >= 0 ? "+" : "") + display;
        }

        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static String formatCategoryName(String raw) {
        if ("demihuman".equals(raw)) {
            return "Demi-Human";
        }

        String[] words = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            if (words[i].isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                builder.append(words[i].substring(1));
            }
        }
        return builder.toString();
    }
}
