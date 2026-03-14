package com.etema.ragnarmmo.system.skills.mage;

import net.minecraft.resources.ResourceLocation;

public class LightningBoltSkillEffect extends ElementalBoltSkillEffect {
    public LightningBoltSkillEffect() {
        super(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "lightning_bolt"), ElementType.WIND);
    }
}
