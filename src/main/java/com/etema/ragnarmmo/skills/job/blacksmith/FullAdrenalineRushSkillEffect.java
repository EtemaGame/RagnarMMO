package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Full Adrenaline Rush — Passive (Blacksmith)
 * RO: Like Adrenaline Rush but also applies its bonuses to party members.
 * MC: Extends the HASTE bonus from Adrenaline Rush to all nearby party players
 *     (within 15 blocks). Stored in PersistentData — BlacksmithSkillEvents
 *     will check this flag when AdrenalineRush fires to broadcast HASTE.
 */
public class FullAdrenalineRushSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "full_adrenaline_rush");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("full_adrenaline_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§c✦ Full Adrenaline Rush §flv." + level
                    + " — Adrenaline Rush también afecta a aliados cercanos (15 bloques)"));
        }
    }
}
