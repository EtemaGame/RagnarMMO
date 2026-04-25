package com.etema.ragnarmmo.skills.job.archer;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

/**
 * Vulture's Eye - passive accuracy/range tuning.
 *
 * Runtime bonuses are resolved in StatComputer and ProjectileSkillHelper. This
 * effect intentionally does not hook damage events; the skill should improve
 * consistency, not grant direct damage.
 */
public class VulturesEyeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "vultures_eye");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
