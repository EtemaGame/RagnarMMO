package com.etema.ragnarmmo.skill.api;

/**
 * Type of skill activation.
 * PASSIVE: Always-on effects, no resource cost.
 * ACTIVE: Triggered effects that consume Mana (magical) or SP (physical).
 */
public enum SkillUsageType {
    PASSIVE,
    ACTIVE
}
