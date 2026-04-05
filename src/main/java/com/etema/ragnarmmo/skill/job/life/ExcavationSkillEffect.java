package com.etema.ragnarmmo.skill.job.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skill.api.ISkillEffect;

/**
 * Effect handler for Excavation skill.
 * Break speed bonuses are applied via {@link LifeSkillBreakSpeedHandler}.
 */
public class ExcavationSkillEffect implements ISkillEffect {

    @Override
    public ResourceLocation getSkillId() {
        return new ResourceLocation("ragnarmmo", "excavation");
    }
}
