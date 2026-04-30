package com.etema.ragnarmmo.skills.job.thief;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class ImproveDodgeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_dodge");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    // Improve Dodge is a stat-derived passive; FLEE must come from the RO stat/profile pipeline.
}
