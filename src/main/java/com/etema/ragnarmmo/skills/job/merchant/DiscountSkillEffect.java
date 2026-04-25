package com.etema.ragnarmmo.skills.job.merchant;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.execution.EconomicSkillHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Discount - Passive (Merchant).
 * Reduces Zeny costs when trading with villagers.
 */
public class DiscountSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "discount");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("discount_level", level);
        if (level > 0) {
            int pct = SkillRegistry.get(ID)
                    .map(def -> (int) Math.round(EconomicSkillHelper.vendorBuyDiscount(def, level) * 100.0D))
                    .orElse(0);
            player.sendSystemMessage(Component.literal(
                    "Discount lv." + level + " - Zeny costs -" + pct + "% at villagers"));
        }
    }
}
