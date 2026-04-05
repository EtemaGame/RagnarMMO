package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Dagger — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of dagas weapons.
 * MC: Stores smith_dagger_level in PersistentData. When the player crafts a dagas weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithDaggerSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "smith_dagger");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_dagger_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Dagger §flv." + level + " — Calidad de dagas crafteado +" + (level * 5) + "%"));
    }
}
