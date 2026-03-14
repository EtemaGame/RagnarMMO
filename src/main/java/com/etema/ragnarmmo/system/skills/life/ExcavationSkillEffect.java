package com.etema.ragnarmmo.system.skills.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.system.skills.ISkillEffect;

/**
 * Effect handler for Excavation skill.
 * Break speed bonuses are applied via {@link LifeSkillBreakSpeedHandler}.
 */
public class ExcavationSkillEffect implements ISkillEffect {

    @Override
    public ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "excavation");
    }
}
