package com.etema.ragnarmmo.skills.resolver;

import com.etema.ragnarmmo.skills.api.SkillCategory;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;

public final class JobSkillEligibilityService {
    private JobSkillEligibilityService() {
    }

    public static boolean canReceiveCombatXp(ResourceLocation skillId) {
        return SkillRegistry.get(skillId)
                .map(def -> def.getCategory() != SkillCategory.LIFE)
                .orElse(true);
    }
}
