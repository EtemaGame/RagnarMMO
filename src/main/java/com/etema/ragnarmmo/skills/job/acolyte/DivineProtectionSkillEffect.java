package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class DivineProtectionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "divine_protection");
    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
