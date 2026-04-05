package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Identify — Active (Merchant)
 * RO: Reveals the stats of unidentified equipment.
 * MC: Reads the NBT tags of the item in the player's main hand and
 *     prints all known RagnarMMO attributes (quality, card slots)
 *     and vanilla attributes in chat — "identifying" the item.
 */
public class IdentifySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "identify");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cIdentify: §fSostén un ítem en la mano."));
            return;
        }

        var nbt = held.getTag();
        player.sendSystemMessage(Component.literal("§e══ Identify: §f" + held.getHoverName().getString() + " §e══"));

        if (nbt == null || nbt.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7(Sin atributos especiales)"));
            return;
        }

        // Show slotted cards using helper
        java.util.List<String> slottedCards = RoItemNbtHelper.getSlottedCards(held);
        if (!slottedCards.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Slotted Cards:"));
            for (int i = 0; i < slottedCards.size(); i++) {
                String cardId = slottedCards.get(i);
                var cardDef = com.etema.ragnarmmo.system.loot.cards.CardRegistry.getInstance().get(cardId);
                String cardName = cardDef != null ? cardDef.displayName() : cardId;
                player.sendSystemMessage(Component.literal("§bSlot " + (i + 1) + ": §f" + cardName));
            }
        }

        // Raw NBT keys for any extra data
        for (String key : nbt.getAllKeys()) {
            if (!key.startsWith("ragnar_") && !key.equals("HideFlags")
                    && !key.equals("Damage") && !key.equals("RepairCost")) {
                player.sendSystemMessage(Component.literal("§7" + key + ": §f" + nbt.get(key)));
            }
        }

        player.sendSystemMessage(Component.literal("§e═══════════════════"));
    }
}
