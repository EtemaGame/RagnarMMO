package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Overcharge — Passive (Merchant)
 * RO: Increases prices when selling to NPCs.
 * MC: Increases emerald rewards when selling items to villagers.
 *     Applied via MerchantSkillEvents when the trade menu is opened.
 */
public class OverchargeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "overcharge");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("overcharge_level", level);
        if (level > 0) {
            int pct = level * 2;
            player.sendSystemMessage(Component.literal(
                    "§6✦ Overcharge §flv." + level + " — Selling to villagers +" + pct + "% emeralds"));
        }
    }
}
