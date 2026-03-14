package com.etema.ragnarmmo.system.skills.merchant;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class EnlargeWeightLimitSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo",
            "enlarge_weight_limit");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
