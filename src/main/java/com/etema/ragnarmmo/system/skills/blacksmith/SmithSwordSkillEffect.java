package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Sword — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of espadas weapons.
 * MC: Stores smith_sword_level in PersistentData. When the player crafts a espadas weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithSwordSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "smith_sword");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_sword_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Sword §flv." + level + " — Calidad de espadas crafteado +" + (level * 5) + "%"));
    }
}