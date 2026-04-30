package com.etema.ragnarmmo.mobs.util;

import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MobAttributeHelper {

    private MobAttributeHelper() {}

    public enum HealthPreservationMode {
        SPAWN_FULL_HEAL,
        PRESERVE_RATIO,
        CLAMP_ONLY
    }

    public static void applyAttributes(LivingEntity mob, MobProfile profile) {
        applyAttributes(mob, profile, HealthPreservationMode.SPAWN_FULL_HEAL);
    }

    public static void applyAttributes(LivingEntity mob, MobProfile profile, HealthPreservationMode healthMode) {
        AttributeInstance maxHealth = getInstance(mob, Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hp = Math.max(1.0D, profile.maxHp());
            double oldMax = Math.max(1.0D, maxHealth.getBaseValue());
            float oldHealth = mob.getHealth();
            boolean wasFull = oldHealth >= (float) oldMax - 0.01F;
            double oldRatio = oldMax > 0.0D ? oldHealth / oldMax : 1.0D;
            maxHealth.setBaseValue(hp);
            HealthPreservationMode mode = healthMode == null ? HealthPreservationMode.SPAWN_FULL_HEAL : healthMode;
            switch (mode) {
                case SPAWN_FULL_HEAL -> {
                    if (mob.tickCount < 10 || wasFull) {
                        mob.setHealth((float) hp);
                    } else {
                        mob.setHealth(Math.min(oldHealth, (float) hp));
                    }
                }
                case PRESERVE_RATIO -> {
                    if (wasFull) {
                        mob.setHealth((float) hp);
                    } else {
                        mob.setHealth((float) Math.max(1.0D, Math.min(hp, hp * oldRatio)));
                    }
                }
                case CLAMP_ONLY -> mob.setHealth(Math.min(oldHealth, (float) hp));
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
