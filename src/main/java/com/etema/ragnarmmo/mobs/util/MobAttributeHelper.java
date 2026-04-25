package com.etema.ragnarmmo.mobs.util;

import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MobAttributeHelper {

    private MobAttributeHelper() {}

    public static void applyAttributes(LivingEntity mob, MobProfile profile) {
        AttributeInstance maxHealth = getInstance(mob, Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hp = Math.max(1.0D, profile.maxHp());
            maxHealth.setBaseValue(hp);
            if (mob.tickCount < 10 || mob.getHealth() >= (float) maxHealth.getBaseValue()) {
                mob.setHealth((float) hp);
            } else {
                mob.setHealth(Math.min(mob.getHealth(), (float) hp));
            }
        }

        AttributeInstance attackDamage = getInstance(mob, Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(Math.max(0.0D, (profile.atkMin() + profile.atkMax()) / 2.0D));
        }

        AttributeInstance armor = getInstance(mob, Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(Math.max(0.0D, profile.def()));
        }

        AttributeInstance movementSpeed = getInstance(mob, Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double speed = Math.min(MobConfigAccess.getMaxMovementSpeed(), Math.max(0.01D, profile.moveSpeed()));
            movementSpeed.setBaseValue(speed);
        }
    }

    private static AttributeInstance getInstance(LivingEntity mob, Attribute attr) {
        return mob.getAttributes().getInstance(attr);
    }
}
