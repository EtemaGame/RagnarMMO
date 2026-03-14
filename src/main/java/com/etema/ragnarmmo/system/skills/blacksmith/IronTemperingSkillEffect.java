package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Iron Tempering — Passive (Blacksmith)
 * RO: Increases physical damage when using Iron weapons.
 * MC: +1% damage per level with Iron/Stone-tier weapons.
 *     Stored in PersistentData; BlacksmithSkillEvents reads it in LivingHurtEvent.
 */
public class IronTemperingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "iron_tempering");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("iron_tempering_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§7✦ Iron Tempering §flv." + level + " — Daño con armas de hierro +" + level + "%"));
        }
    }
}
