package com.etema.ragnarmmo.skill.execution.aoe;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.targeting.SkillTargeting;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for skills that target a position on the ground and create a persistent area effect.
 * Examples: Sanctuary, Magnus Exorcismus, Fire Pillar.
 */
public abstract class GroundAoEPersistentEffect implements ISkillEffect {
    protected final ResourceLocation skillId;

    protected GroundAoEPersistentEffect(ResourceLocation skillId) {
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
        Vec3 strikePos = SkillTargeting.resolveStrikePosition(user, range);
        int animDelay = getAnimationDelay(level);

        playCastVisuals(user, strikePos, level);

        if (animDelay <= 0) {
            spawnAoE(user, strikePos, level);
        } else {
            SkillSequencer.schedule(animDelay, () -> {
                if (user.isAlive()) {
                    spawnAoE(user, strikePos, level);
                }
            });
        }
    }

    /**
     * @return Maximum range to place the AoE.
     */
    protected abstract double getRange(int level);

    /**
     * @return Delay in ticks before the AoE is spawned (animation swing).
     */
    protected int getAnimationDelay(int level) {
        return 0;
    }

    /**
     * Plays visuals during the casting phase or at the moment of placement.
     */
    protected abstract void playCastVisuals(LivingEntity user, Vec3 pos, int level);

    /**
     * Spawns the persistent AoE entity or schedules periodic effects.
     */
    protected abstract void spawnAoE(LivingEntity user, Vec3 pos, int level);
}
