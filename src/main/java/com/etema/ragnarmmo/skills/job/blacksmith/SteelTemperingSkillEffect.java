package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Steel Tempering — Passive (Blacksmith)
 * RO: Increases weapon ATK when using Steel (Oridecon-refined) weapons.
 * MC: +1.5% damage (as 1 per 2 levels, rounded) with Diamond/Netherite weapons.
 *     PassiveCombatModifierService resolves it inside the RO combat contract.
 */
public class SteelTemperingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "steel_tempering");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("steel_tempering_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§b✦ Steel Tempering §flv." + level + " — Daño con armas Diamante/Netherite +" + (int)(level * 1.5) + "%"));
        }
    }
}
