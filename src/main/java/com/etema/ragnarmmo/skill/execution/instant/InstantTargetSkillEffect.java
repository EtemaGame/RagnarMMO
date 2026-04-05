package com.etema.ragnarmmo.skill.execution.instant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.targeting.SkillTargeting;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for skills that target a single entity (or self) and apply an effect almost immediately.
 * Examples: Bash, Mammonite, Heal, Steal.
 */
public abstract class InstantTargetSkillEffect implements ISkillEffect {
    protected final ResourceLocation skillId;

    protected InstantTargetSkillEffect(ResourceLocation skillId) {
        this.skillId = skillId;
    }

    @Override
    public ResourceLocation getSkillId() {
        return skillId;
    }

    @Override
    public void execute(LivingEntity user, int level) {
        if (level <= 0) return;

        double range = getRange(level);
        LivingEntity target = getTarget(user, range);
        int delay = getAnimationDelay(level);

        playInitialVisuals(user, target, level);

        if (delay <= 0) {
            applyEffect(user, target, level);
        } else {
            SkillSequencer.schedule(delay, () -> {
                if (user.isAlive()) {
                    applyEffect(user, target, level);
                }
            });
        }
    }

    /**
     * @return Maximum range to search for a target.
     */
    protected abstract double getRange(int level);

    /**
     * @return Delay in ticks before applyEffect is called (swing time animation).
     */
    protected int getAnimationDelay(int level) {
        return 0;
    }

    /**
     * Finds the target for the skill. Override if specific targeting logic is needed (e.g. only allies).
     */
    @Nullable
    protected LivingEntity getTarget(LivingEntity user, double range) {
        return SkillTargeting.findEntityInSight(user, range);
    }

    /**
     * Plays visuals as soon as the skill is activated (e.g. casting particles, swing sounds).
     */
    protected abstract void playInitialVisuals(LivingEntity user, @Nullable LivingEntity target, int level);

    /**
     * Applies the actual skill effect (damage, heal, status) after the execution delay.
     */
    protected abstract void applyEffect(LivingEntity user, @Nullable LivingEntity target, int level);
}
