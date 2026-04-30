package com.etema.ragnarmmo.combat.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Final application adapter for contract-resolved skill damage.
 */
public final class SkillDamageHelper {

    private SkillDamageHelper() {}

    public static boolean dealSkillDamage(LivingEntity target, DamageSource source, float amount) {
        if (!target.isAlive() || amount <= 0)
            return false;

        int savedHurtTime = target.hurtTime;
        int savedHurtDuration = target.hurtDuration;
        int savedInvulnerableTime = target.invulnerableTime;

        target.hurtTime = 0;
        target.hurtDuration = 0;
        target.invulnerableTime = 0;

        boolean hit = target.hurt(source, amount);

        if (target.isAlive()) {
            target.hurtTime = Math.max(target.hurtTime, savedHurtTime);
            target.hurtDuration = Math.max(target.hurtDuration, savedHurtDuration);

            // Skill multihits should not create a fresh 20-tick vanilla cooldown when
            // the target was otherwise hittable. We only preserve a pre-existing
            // invulnerability window if one already existed before this hit.
            if (savedInvulnerableTime <= 0) {
                target.invulnerableTime = 0;
            }
        }

        return hit;
    }
}
