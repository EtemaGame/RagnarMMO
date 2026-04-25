package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smith Spear — Passive (Blacksmith)
 * RO: Allows crafting and increases quality of lanzas/tridentes weapons.
 * MC: Stores smith_spear_level in PersistentData. When the player crafts a lanzas/tridentes weapon
 *     (future CraftingEvent hook), the item gains +level bonus durability
 *     via NBT.
 */
public class SmithSpearSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "smith_spear");
    @Override public ResourceLocation getSkillId() { return ID; }
    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("smith_spear_level", level);
        if (level > 0)
            player.sendSystemMessage(Component.literal("§e✦ Smith Spear §flv." + level + " — Calidad de lanzas/tridentes crafteado +" + (level * 5) + "%"));
    }
}
