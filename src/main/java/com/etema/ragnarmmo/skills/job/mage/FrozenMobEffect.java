package com.etema.ragnarmmo.skills.job.mage;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen — Harmful Status Effect
 * RO: Target becomes immobile and changes property to Water 1.
 * Wind attacks deal 175% damage, Fire attacks deal 40% damage.
 */
public class FrozenMobEffect extends MobEffect {
    
    public FrozenMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x87CEEB); // Sky Blue
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F1608A0", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
