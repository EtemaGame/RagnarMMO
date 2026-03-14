package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Axe — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of hachas weapons.
 * MC: Stores smith_axe_level in PersistentData. When the player crafts a hachas weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithAxeSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "smith_axe");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_axe_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Axe §flv." + level + " — Calidad de hachas crafteado +" + (level * 5) + "%"));
    }
}