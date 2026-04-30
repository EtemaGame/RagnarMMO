package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

/**
 * Two-Hand Mastery — Passive
 * RO: +4 ATK per level when wielding 2-Handed Swords.
 * Minecraft adaptation: adds a flat +4 damage per level to outgoing 2H sword hits.
 */
public class TwoHandMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "two_hand_mastery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
