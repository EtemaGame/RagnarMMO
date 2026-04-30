package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Research Weaponry — Passive (Blacksmith)
 * RO: Increases ATK with all weapon types.
 * MC: Flat +damage per level with ANY weapon (sword/axe/pickaxe/etc).
 *     HandAttackProfileResolver feeds this into the RO attack profile.
 */
public class ResearchWeaponrySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "research_weaponry");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("research_weaponry_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§e✦ Research Weaponry §flv." + level + " — ATK +" + (int)(level * 0.5 * 2) / 2.0 + " con cualquier arma"));
        }
    }
}
