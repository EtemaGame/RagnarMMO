package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

/**
 * Steel Crow — Passive
 * RO: Increases the damage of Blitz Beat.
 * 
 * In Minecraft:
 * - This skill provides a flat damage bonus to each hit of Blitz Beat.
 * - Bonus = +6 damage per level (Level 10 = +60 damage per hit).
 */
public class SteelCrowSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "steel_crow");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    // This is a passive skill whose level is queried by BlitzBeatSkillEffect.
    // No active execution logic needed here.
}
