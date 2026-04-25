package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Research Oridecon — Passive (Blacksmith)
 * RO: Allows refining weapons with Oridecon.
 * MC: Passive that increases drop chance of metal ores/ingots from mobs
 *     by (level * 10)%. Stored in PersistentData; RagnarLootModifier reads
 *     research_oridecon_level to apply extra loot rolls for iron/gold/diamond mobs.
 */
public class ResearchOrideconSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "research_oridecon");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("research_oridecon_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§6✦ Research Oridecon §flv." + level + " — Drop de metales +" + (level * 10) + "% de mobs"));
        }
    }
}
