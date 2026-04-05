package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.skill.execution.projectile.AbstractBoltSkillEffect;
import net.minecraft.resources.ResourceLocation;

public final class FireBoltSkillEffect extends AbstractBoltSkillEffect {
    public FireBoltSkillEffect(ResourceLocation id) {
        super(id, ElementType.FIRE);
    }
}
