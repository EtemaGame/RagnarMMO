package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Mace — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of mazas/picos weapons.
 * MC: Stores smith_mace_level in PersistentData. When the player crafts a mazas/picos weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithMaceSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "smith_mace");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_mace_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Mace §flv." + level + " — Calidad de mazas/picos crafteado +" + (level * 5) + "%"));
    }
}
