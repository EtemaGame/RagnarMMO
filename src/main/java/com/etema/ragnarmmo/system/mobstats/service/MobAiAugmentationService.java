package com.etema.ragnarmmo.system.mobstats.service;

import com.etema.ragnarmmo.system.mobstats.ai.MobUseSkillGoal;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.Objects;

/**
 * Service for augmenting mob AI with specialized goals and skills.
 */
public final class MobAiAugmentationService {

    private MobAiAugmentationService() {
    }

    /**
     * Adds AI goals to a mob based on its semantic class.
     */
    public static void addMobClassAI(Mob mob, MobClass mobClass) {
        Objects.requireNonNull(mob, "mob");
        if (mobClass == null) {
            return;
        }

        switch (mobClass) {
            case SWORDMAN:
                // Bash: Range 3.5, CD 4s (80 ticks), Mana 15
                mob.goalSelector.addGoal(2, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "bash"), 3.5, 80, 15));
                break;
            case ACOLYTE:
                // Heal: Range 8.0, CD 5s (100 ticks), Mana 20
                mob.goalSelector.addGoal(3, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "heal"), 8.0, 100, 20));
                break;
            case MAGE:
                // Fire Bolt: Range 12.0, CD 3s (60 ticks), Mana 20
                mob.goalSelector.addGoal(2, new MobUseSkillGoal(mob, new ResourceLocation("ragnarmmo", "fire_bolt"), 12.0, 60, 20));
                break;
            default:
                break;
        }
    }
}
