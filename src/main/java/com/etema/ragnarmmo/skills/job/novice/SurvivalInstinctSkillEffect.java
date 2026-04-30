package com.etema.ragnarmmo.skills.job.novice;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skills.api.ISkillEffect;

public class SurvivalInstinctSkillEffect implements ISkillEffect {

    // Tag en NBT del jugador para cooldown
    private static final String TAG_CD_UNTIL = "ragnarmmo_survival_cd_until";

    @Override
    public ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "survival_instinct");
    }

}
