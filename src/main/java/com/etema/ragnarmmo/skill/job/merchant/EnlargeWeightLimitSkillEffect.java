package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class EnlargeWeightLimitSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "enlarge_weight_limit");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
