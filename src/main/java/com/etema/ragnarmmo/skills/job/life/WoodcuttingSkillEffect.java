package com.etema.ragnarmmo.skills.job.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skills.api.ISkillEffect;

/**
 * Effect handler for Woodcutting skill.
 * Break speed bonuses are applied via {@link LifeSkillBreakSpeedHandler}.
 */
public class WoodcuttingSkillEffect implements ISkillEffect {

    @Override
    public ResourceLocation getSkillId() {
        return new ResourceLocation("ragnarmmo", "woodcutting");
    }
}
