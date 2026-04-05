package com.etema.ragnarmmo.entity;

import net.minecraft.resources.ResourceLocation;

/**
 * Interface for entities that represent visual skill effects and provide a Skill ID.
 */
public interface IVisualSkillEntity {
    ResourceLocation getSkillId();
    void ambientParticles();
}
