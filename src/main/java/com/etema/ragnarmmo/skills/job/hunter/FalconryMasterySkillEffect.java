package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Falconry Mastery — Passive
 * RO: Permits the use of a Falcon.
 * 
 * In Minecraft:
 * - This skill acts as a prerequisite for Blitz Beat and other Falcon skills.
 * - Having at least Level 1 enables the Falcon following the player (visual/logic flag).
 */
public class FalconryMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "falconry_mastery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    public void onSkillLeveled(ServerPlayer player, int level) {
        // Potential logic to spawn a decorative Falcon entity or set a persistent flag
        if (level > 0) {
            player.getPersistentData().putBoolean("ragnar_has_falcon", true);
        }
    }
}
