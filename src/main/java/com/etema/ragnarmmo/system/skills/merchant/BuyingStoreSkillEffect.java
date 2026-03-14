package com.etema.ragnarmmo.system.skills.merchant;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class BuyingStoreSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "buying_store");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
