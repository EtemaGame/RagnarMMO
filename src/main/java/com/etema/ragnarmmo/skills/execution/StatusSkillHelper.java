package com.etema.ragnarmmo.skills.execution;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class StatusSkillHelper {
    private StatusSkillHelper() {
    }

    public static boolean tryApplyStatus(LivingEntity target, MobEffect effect, int durationTicks, int amplifier,
            double chancePercent, RandomSource random) {
        if (target == null || effect == null || durationTicks <= 0) {
            return false;
        }
        double clampedChance = Math.max(0.0D, Math.min(100.0D, chancePercent));
        if (random.nextDouble() * 100.0D > clampedChance) {
            return false;
        }
        target.addEffect(new MobEffectInstance(effect, durationTicks, Math.max(0, amplifier), false, false, true));
        return true;
    }
}
