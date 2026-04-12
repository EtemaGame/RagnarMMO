package com.etema.ragnarmmo.skill.execution;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class BuffSkillHelper {
    private BuffSkillHelper() {
    }

    public static void applyMobEffect(LivingEntity target, MobEffect effect, int durationTicks, int amplifier) {
        if (target == null || effect == null || durationTicks <= 0) {
            return;
        }
        target.addEffect(new MobEffectInstance(effect, durationTicks, Math.max(0, amplifier), false, false, true));
    }
}
