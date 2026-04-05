package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class BuyingStoreSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "buying_store");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
