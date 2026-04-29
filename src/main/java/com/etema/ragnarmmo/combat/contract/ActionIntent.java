package com.etema.ragnarmmo.combat.contract;

import net.minecraft.resources.ResourceLocation;

/**
 * Describes what an actor is trying to do. Damage is still resolved by the
 * combat contract, not by the producer of the intent.
 */
public sealed interface ActionIntent permits ActionIntent.BasicAttackIntent, ActionIntent.SkillIntent {
    record BasicAttackIntent(boolean offHand) implements ActionIntent {
    }

    /**
     * Future-facing placeholder for P2. P0 defines the shape but does not
     * migrate skills yet.
     */
    record SkillIntent(ResourceLocation skillId, int skillLevel, Integer targetEntityId) implements ActionIntent {
    }
}
