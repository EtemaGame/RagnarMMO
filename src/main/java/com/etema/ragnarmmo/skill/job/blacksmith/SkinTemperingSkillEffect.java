package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Skin Tempering — Passive (Blacksmith)
 * RO: Reduces Fire and Normal damage taken.
 * MC: Reduces fire damage by (level * 5)%.
 *     Stored in PersistentData; BlacksmithSkillEvents uses it in LivingDamageEvent
 *     to reduce damage when the source is fire/lava/burning.
 */
public class SkinTemperingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "skin_tempering");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("skin_tempering_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§c✦ Skin Tempering §flv." + level + " — Daño de fuego recibido –" + (level * 5) + "%"));
        }
    }
}
