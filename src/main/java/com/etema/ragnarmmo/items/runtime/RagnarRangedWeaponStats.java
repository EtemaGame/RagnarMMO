package com.etema.ragnarmmo.items.runtime;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for weapons that provide specific ranged combat stats.
 * Used for Bows, Crossbows, and future Firearms.
 */
public interface RagnarRangedWeaponStats {
    /**
     * @return The base weapon attack for ranged combat.
     */
    double getRangedWeaponAtk(ItemStack stack);

    /**
     * @return The base ASPD (RO scale, 0-190) for this weapon.
     */
    int getBaseRangedAspd(ItemStack stack);

    /**
     * @return The base ticks required for a full draw/charge.
     */
    int getBaseDrawTicks(ItemStack stack);

    /**
     * @return The multiplier for the projectile's velocity.
     */
    float getProjectileVelocity(ItemStack stack);
}
