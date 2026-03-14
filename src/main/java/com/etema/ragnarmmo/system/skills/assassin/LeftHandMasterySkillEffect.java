package com.etema.ragnarmmo.system.skills.assassin;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Left Hand Mastery — Passive (Assassin)
 * RO: Reduces the ATK penalty when equipping a weapon in the offhand.
 * MC: Increases offhand damage contribution per level.
 *     Works in tandem with Dual Wield — stored in PersistentData.
 *     AssassinSkillEvents uses: offhand_bonus_pct = base + (left_mastery * 5%).
 */
public class LeftHandMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "left_hand_mastery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("left_mastery_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§4✦ Left Hand Mastery §flv." + level + " — Daño offhand +" + (level * 5) + "% adicional"));
        }
    }
}
