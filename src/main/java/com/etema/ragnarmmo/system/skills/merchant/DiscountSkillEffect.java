package com.etema.ragnarmmo.system.skills.merchant;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Discount — Passive (Merchant)
 * RO: Reduces NPC buy prices.
 * MC: Reduces emerald costs when trading with villagers.
 *     Applied via MerchantSkillEvents when the trade menu is opened.
 */
public class DiscountSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "discount");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("discount_level", level);
        if (level > 0) {
            int pct = 2 + level * 3;
            player.sendSystemMessage(Component.literal(
                    "§a✦ Discount §flv." + level + " — Emerald costs ‑" + pct + "% at villagers"));
        }
    }
}
