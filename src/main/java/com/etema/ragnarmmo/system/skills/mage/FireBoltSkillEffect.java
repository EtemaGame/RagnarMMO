package com.etema.ragnarmmo.system.skills.mage;

import net.minecraft.resources.ResourceLocation;

public class FireBoltSkillEffect extends ElementalBoltSkillEffect {
    public FireBoltSkillEffect() {
        super(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "fire_bolt"), ElementType.FIRE);
    }
}
