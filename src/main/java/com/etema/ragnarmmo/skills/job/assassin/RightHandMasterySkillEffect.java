package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Right Hand Mastery — Passive (Assassin)
 * RO: Increases ATK with the right (main) hand weapon when dual wielding.
 * MC: +5% main hand damage per level while dual wielding.
 *     Stored in PersistentData. Works as a multiplier stacked on top of
 *     Dual Wield when AssassinSkillEvents computes final damage.
 */
public class RightHandMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "right_hand_mastery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("right_mastery_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§4✦ Right Hand Mastery §flv." + level + " — Daño mano principal +" + (level * 5) + "% (Dual Wield)"));
        }
    }
}
