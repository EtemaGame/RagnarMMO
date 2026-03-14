package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

public class ImproveDodgeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_dodge");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    // Improve Dodge: Passively increases Flee (+3 to +30) and Movement Speed for
    // Assassins.
    // In Minecraft, this would translate directly to generic.movement_speed
    // modifier or a custom Flee attribute.
    // Logic will hook into the attribute calculation system or be checked
    // defensively (LivingAttackEvent) to evade hits.
}
