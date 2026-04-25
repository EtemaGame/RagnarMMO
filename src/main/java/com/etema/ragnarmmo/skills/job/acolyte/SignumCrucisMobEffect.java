package com.etema.ragnarmmo.skills.job.acolyte;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class SignumCrucisMobEffect extends MobEffect {
    private static final double[] DEF_REDUCTION = {
            -0.14D, -0.18D, -0.22D, -0.26D, -0.30D,
            -0.34D, -0.38D, -0.42D, -0.46D, -0.50D
    };

    public SignumCrucisMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xD7D7F9);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        int index = Math.max(0, Math.min(DEF_REDUCTION.length - 1, amplifier));
        return DEF_REDUCTION[index];
    }
}
