package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Two-Handed Sword — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of espadas 2H weapons.
 * MC: Stores smith_2h_sword_level in PersistentData. When the player crafts a espadas 2H weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithTwoHandedSwordSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "smith_two_handed_sword");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_2h_sword_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Two-Handed Sword §flv." + level + " — Calidad de espadas 2H crafteado +" + (level * 5) + "%"));
    }
}
