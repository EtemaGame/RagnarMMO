package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Magnum Break — Active (Fire AoE)
 * RO: Deals Fire property physical damage in a 5x5 area with knockback.
 *     Grants +20% Fire property damage bonus for 10 seconds after cast.
 *
 * Minecraft:
 *  - Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
 */
public class MagnumBreakSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "magnum_break");
    public static final String FIRE_BUFF_TAG = "ragnar_magnum_fire_until";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(LivingEntity user, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private float defaultHpCost(int level) {
        return switch (level) {
            case 1, 2 -> 20.0f;
            case 3, 4 -> 19.0f;
            case 5, 6 -> 18.0f;
            case 7, 8 -> 17.0f;
            default -> 16.0f;
        };
    }
}
