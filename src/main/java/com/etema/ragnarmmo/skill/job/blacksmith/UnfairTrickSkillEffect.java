package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Unfair Trick — Passive (Blacksmith)
 * RO: Increases damage against targets with low HP.
 * MC: +level*3% damage bonus when the target has less than 50% of max HP.
 *     BlacksmithSkillEvents applies this in LivingHurtEvent.
 */
public class UnfairTrickSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "unfair_trick");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("unfair_trick_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§4✦ Unfair Trick §flv." + level + " — Bonus +" + (level * 3) + "% daño a objetivos <50% HP"));
        }
    }
}
