package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Brass Knuckle — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of nudilleras weapons.
 * MC: Stores smith_knuckle_level in PersistentData. When the player crafts a nudilleras weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability and
 *     a minor damage bonus via NBT affixes.
 */
public class SmithBrassKnuckleSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "smith_brass_knuckle");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_knuckle_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Brass Knuckle §flv." + level + " — Calidad de nudilleras crafteado +" + (level * 5) + "%"));
    }
}
