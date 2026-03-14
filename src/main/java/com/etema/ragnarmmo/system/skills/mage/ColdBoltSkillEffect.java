package com.etema.ragnarmmo.system.skills.mage;

import net.minecraft.resources.ResourceLocation;

public class ColdBoltSkillEffect extends ElementalBoltSkillEffect {
    public ColdBoltSkillEffect() {
        super(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "cold_bolt"), ElementType.WATER);
    }
}
