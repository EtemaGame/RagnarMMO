package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Steel Tempering — Passive (Blacksmith)
 * RO: Increases weapon ATK when using Steel (Oridecon-refined) weapons.
 * MC: +1.5% damage (as 1 per 2 levels, rounded) with Diamond/Netherite weapons.
 *     BlacksmithSkillEvents reads steel_tempering_level in LivingHurtEvent.
 */
public class SteelTemperingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "steel_tempering");

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
