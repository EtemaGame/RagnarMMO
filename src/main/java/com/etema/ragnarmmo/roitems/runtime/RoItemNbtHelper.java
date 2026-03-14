package com.etema.ragnarmmo.roitems.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class RoItemNbtHelper {

    public static final String TAG_SLOTTED_CARDS = "RoSlottedCards";



    /**
     * Gets a list of card IDs currently slotted in the item.
     * 
     * @param stack the equipment item
     * @return a list of card IDs (empty if none)
     */
    public static List<String> getSlottedCards(ItemStack stack) {
        List<String> cards = new ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains(TAG_SLOTTED_CARDS, Tag.TAG_LIST)) {
            return cards;
        }

        ListTag cardList = stack.getTag().getList(TAG_SLOTTED_CARDS, Tag.TAG_STRING);
        for (int i = 0; i < cardList.size(); i++) {
            cards.add(cardList.getString(i));
        }
        return cards;
    }

    /**
     * Adds a card to the item's slotted cards array.
     * Note: This does not verify maximum slots (that should be checked before
     * calling).
     * 
     * @param stack  the equipment item
     * @param cardId the card ID to insert
     */
    public static void addSlottedCard(ItemStack stack, String cardId) {
        if (stack.isEmpty() || cardId == null || cardId.isEmpty())
            return;

        CompoundTag tag = stack.getOrCreateTag();
        ListTag cardList;
        if (tag.contains(TAG_SLOTTED_CARDS, Tag.TAG_LIST)) {
            cardList = tag.getList(TAG_SLOTTED_CARDS, Tag.TAG_STRING);
        } else {
            cardList = new ListTag();
            tag.put(TAG_SLOTTED_CARDS, cardList);
        }

        cardList.add(StringTag.valueOf(cardId));
    }
}
