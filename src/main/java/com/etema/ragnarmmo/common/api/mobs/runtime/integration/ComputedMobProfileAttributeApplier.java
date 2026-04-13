package com.etema.ragnarmmo.common.api.mobs.runtime.integration;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobBaseStats;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Objects;

/**
 * Applies a resolved manual {@link ComputedMobProfile} to live mob attributes.
 *
 * <p>This is a narrow runtime bridge for the new manual path only. It does not introduce a second
 * authority and does not mutate legacy {@code MobStats} state.</p>
 */
public final class ComputedMobProfileAttributeApplier {

    private ComputedMobProfileAttributeApplier() {
    }

    public static void apply(LivingEntity entity, ComputedMobProfile profile) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(profile, "profile");

        setBaseValue(entity, Attributes.MAX_HEALTH, profile.maxHp());
        setBaseValue(entity, Attributes.ATTACK_DAMAGE, averageAttack(profile));
        setBaseValue(entity, Attributes.ARMOR, profile.def());
        setBaseValue(entity, Attributes.MOVEMENT_SPEED,
                Math.min(MobStatsConfigAccess.getMaxMovementSpeed(), Math.max(0.01D, profile.moveSpeed())));
        setBaseValue(entity, RagnarAttributes.MAGIC_DEFENSE.get(), profile.mdef());

        ComputedMobBaseStats baseStats = profile.baseCombatStats();
        if (baseStats != null) {
            setBaseValue(entity, RagnarAttributes.STR.get(), baseStats.str() != null ? baseStats.str() : 1);
            setBaseValue(entity, RagnarAttributes.AGI.get(), baseStats.agi());
            setBaseValue(entity, RagnarAttributes.VIT.get(), baseStats.vit());
            setBaseValue(entity, RagnarAttributes.INT.get(), baseStats.intelligence());
            setBaseValue(entity, RagnarAttributes.DEX.get(), baseStats.dex() != null ? baseStats.dex() : 1);
            setBaseValue(entity, RagnarAttributes.LUK.get(), baseStats.luk());
            setBaseValue(entity, Attributes.KNOCKBACK_RESISTANCE,
                    Math.min(1.0D, baseStats.luk() * MobStatsConfigAccess.getLukToKbResist()));
        }

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            float appliedMaxHealth = (float) maxHealth.getBaseValue();
            if (entity.tickCount < 10 || entity.getHealth() >= appliedMaxHealth) {
                entity.setHealth(appliedMaxHealth);
            } else {
                entity.setHealth(Math.min(entity.getHealth(), appliedMaxHealth));
            }
        }
    }

    private static double averageAttack(ComputedMobProfile profile) {
        return (profile.atkMin() + profile.atkMax()) / 2.0D;
    }

    private static void setBaseValue(LivingEntity entity, Attribute attribute, double value) {
        if (attribute == null) {
            return;
        }

        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
