package com.etema.ragnarmmo.items.network;

import com.etema.ragnarmmo.items.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.items.runtime.RoEquipmentTypeResolver;
import com.etema.ragnarmmo.items.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.system.loot.cards.CardItem;
import com.etema.ragnarmmo.system.loot.cards.CardRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when compounding a card into equipment.
 */
public class CardCompoundPacket {

    private final int cardSlot;
    private final int equipSlot;

    public CardCompoundPacket(int cardSlot, int equipSlot) {
        this.cardSlot = cardSlot;
        this.equipSlot = equipSlot;
    }

    public static void encode(CardCompoundPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.cardSlot);
        buf.writeInt(msg.equipSlot);
    }

    public static CardCompoundPacket decode(FriendlyByteBuf buf) {
        return new CardCompoundPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(CardCompoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;

            ItemStack cardStack = player.getInventory().getItem(msg.cardSlot);
            ItemStack equipStack = player.getInventory().getItem(msg.equipSlot);

            // Validation
            if (cardStack.isEmpty() || !(cardStack.getItem() instanceof CardItem))
                return;
            if (equipStack.isEmpty())
                return;

            String cardId = CardItem.getCardId(cardStack);
            var cardDef = cardId != null ? CardRegistry.getInstance().get(cardId) : null;
            if (cardDef == null)
                return;

            if (!RoEquipmentTypeResolver.isCompatible(cardDef.equipType(), equipStack)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "That card cannot be inserted into this equipment."));
                return;
            }

            int maxSlots = RoItemRuleResolver.resolve(equipStack).cardSlots();
            if (maxSlots <= 0)
                return;

            int currentSlots = RoItemNbtHelper.getSlottedCards(equipStack).size();
            if (currentSlots >= maxSlots)
                return;

            // Consume card and slot it
            cardStack.shrink(1);
            RoItemNbtHelper.addSlottedCard(equipStack, cardId);

            player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS,
                    1.0F, 1.0F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 0.5F, 1.5F);
        });
        ctx.get().setPacketHandled(true);
    }
}
