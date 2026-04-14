package com.etema.ragnarmmo.system.mobstats.service;

import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Legacy heuristic resolver for assigning internal classes when no authoritative runtime profile exists.
 */
public final class LegacyMobClassFallbackResolver {

    private LegacyMobClassFallbackResolver() {
    }

    public static MobClass resolve(LivingEntity mob) {
        if (mob instanceof Zombie) return MobClass.SWORDMAN;
        if (mob instanceof Skeleton) return MobClass.ARCHER;
        if (mob instanceof Spider || mob instanceof Creeper || mob instanceof EnderMan) return MobClass.THIEF;
        if (mob instanceof Blaze || mob instanceof Witch) return MobClass.MAGE;

        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance spd = mob.getAttribute(Attributes.MOVEMENT_SPEED);

        if (atk != null && atk.getBaseValue() > 3) return MobClass.SWORDMAN;
        if (spd != null && spd.getBaseValue() > 0.3) return MobClass.THIEF;
        return MobClass.NOVICE;
    }
}
