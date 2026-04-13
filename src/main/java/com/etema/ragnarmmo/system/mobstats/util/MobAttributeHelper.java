package com.etema.ragnarmmo.system.mobstats.util;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MobAttributeHelper {

    private static final String ORIGINAL_MOVE_SPEED_KEY = "RagnarOriginalMoveSpeed";
    
    private MobAttributeHelper() {}

    public static void applyAttributes(LivingEntity mob, MobStats stats) {
        int level = stats.getLevel();
        int str = stats.get(StatKeys.STR);
        int agi = stats.get(StatKeys.AGI);
        int vit = stats.get(StatKeys.VIT);
        int intel = stats.get(StatKeys.INT);
        int dex = stats.get(StatKeys.DEX);
        int luk = stats.get(StatKeys.LUK);

        // Max Health
        AttributeInstance maxHealth = getInstance(mob, Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hp = (MobStatsConfigAccess.getHpBase()
                    + vit * MobStatsConfigAccess.getVitToHp()
                    + level * MobStatsConfigAccess.getLevelToHp()) * stats.getHealthMultiplier();
            hp = Math.max(1.0D, hp);
            maxHealth.setBaseValue(hp);
            // If spawning or health is at max, keep it at max. Always clamp to avoid overflow.
            if (mob.tickCount < 10 || mob.getHealth() >= (float)maxHealth.getBaseValue()) {
                mob.setHealth((float) hp);
            } else {
                mob.setHealth(Math.min(mob.getHealth(), (float)hp));
            }
        }

        // Damage
        AttributeInstance attackDamage = getInstance(mob, Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            double atk = (MobStatsConfigAccess.getAtkBase()
                    + str * MobStatsConfigAccess.getStrToAtk()
                    + dex * MobStatsConfigAccess.getDexToAtk()
                    + luk * MobStatsConfigAccess.getLukToAtk()) * stats.getDamageMultiplier();
            attackDamage.setBaseValue(Math.max(0.0D, atk));
        }

        // Armor
        AttributeInstance armor = getInstance(mob, Attributes.ARMOR);
        if (armor != null) {
            double armorValue = (MobStatsConfigAccess.getArmorBase()
                    + vit * MobStatsConfigAccess.getVitToArmor()
                    + intel * MobStatsConfigAccess.getIntToArmor()) * stats.getDefenseMultiplier();
            armor.setBaseValue(Math.max(0.0D, armorValue));
        }

        // Speed
        AttributeInstance movementSpeed = getInstance(mob, Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double base = mob.getPersistentData().contains(ORIGINAL_MOVE_SPEED_KEY)
                    ? mob.getPersistentData().getDouble(ORIGINAL_MOVE_SPEED_KEY)
                    : movementSpeed.getBaseValue();
            if (base <= 0.0D) {
                base = movementSpeed.getBaseValue();
            }

            mob.getPersistentData().putDouble(ORIGINAL_MOVE_SPEED_KEY, base);

            double spd = (base + agi * MobStatsConfigAccess.getAgiToSpeed()) * stats.getSpeedMultiplier();
            spd = Math.min(MobStatsConfigAccess.getMaxMovementSpeed(), Math.max(0.01D, spd));
            movementSpeed.setBaseValue(spd);
        }

        // Knockback Resistance
        AttributeInstance knockback = getInstance(mob, Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            double kb = Math.min(1.0D, luk * MobStatsConfigAccess.getLukToKbResist());
            knockback.setBaseValue(kb);
        }
    }

    public static void applyAttributes(LivingEntity mob, com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile profile) {
        // Max Health
        AttributeInstance maxHealth = getInstance(mob, Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hp = (double) profile.maxHp();
            maxHealth.setBaseValue(hp);
            mob.setHealth((float) hp);
        }

        // Damage
        AttributeInstance attackDamage = getInstance(mob, Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue((double) profile.atkMax());
        }

        // Armor
        AttributeInstance armor = getInstance(mob, Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue((double) profile.def());
        }

        // Speed
        AttributeInstance movementSpeed = getInstance(mob, Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(profile.moveSpeed());
        }
    }

    private static AttributeInstance getInstance(LivingEntity mob, Attribute attr) {
        return mob.getAttributes().getInstance(attr);
    }
}
