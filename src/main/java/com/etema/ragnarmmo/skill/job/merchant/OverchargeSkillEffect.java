package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.execution.EconomicSkillHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Overcharge - Passive (Merchant).
 * Increases Zeny rewards when selling to villagers.
 */
public class OverchargeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "overcharge");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("overcharge_level", level);
        if (level > 0) {
            int pct = SkillRegistry.get(ID)
                    .map(def -> (int) Math.round(EconomicSkillHelper.vendorSellBonus(def, level) * 100.0D))
                    .orElse(0);
            player.sendSystemMessage(Component.literal(
                    "Overcharge lv." + level + " - selling to villagers +" + pct + "% Zeny"));
        }
    }
}
