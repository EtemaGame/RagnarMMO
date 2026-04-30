package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;

/**
 * Sword Mastery — Passive
 * RO: +4 ATK per level when wielding 1-Handed Swords.
 * Minecraft adaptation: adds a flat +4 damage per level to outgoing sword hits.
 */
public class SwordMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sword_mastery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }
}
